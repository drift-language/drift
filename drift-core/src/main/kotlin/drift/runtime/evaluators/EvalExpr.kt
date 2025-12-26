/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.evaluators

import drift.ast.expressions.Assign
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.Conditional
import drift.ast.expressions.DrExpr
import drift.ast.statements.DrStmt
import drift.ast.statements.Function
import drift.ast.expressions.Get
import drift.ast.expressions.Lambda
import drift.ast.expressions.ListLiteral
import drift.ast.expressions.Literal
import drift.ast.expressions.Set
import drift.ast.expressions.Unary
import drift.ast.expressions.Variable
import drift.ast.statements.FunctionParameter
import drift.runtime.values.callables.DrFunction
import drift.runtime.values.callables.DrLambda
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.callables.DrReturn
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.helper.evalCondition
import drift.helper.unwrap
import drift.helper.validateValue
import drift.runtime.*
import drift.runtime.values.containers.list.DrList
import drift.runtime.values.containers.range.DrExclusiveRange
import drift.runtime.values.containers.range.DrInclusiveRange
import drift.runtime.values.containers.range.DrRange
import drift.runtime.values.imports.DrModule
import drift.runtime.values.oop.DrClass
import drift.runtime.values.oop.DrInstance
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrNumeric
import drift.runtime.values.primaries.DrString
import drift.runtime.values.primaries.DrUInt
import drift.runtime.values.primaries.promoteNumericPair
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.specials.DrNull
import drift.runtime.values.specials.DrVoid
import drift.runtime.values.variables.DrVariable
import drift.utils.castNumericIfNeeded


/******************************************************************************
 * DRIFT EXPRESSIONS EVALUATOR
 *
 * This evaluator computes all Drift expressions.
 ******************************************************************************/



/**
 * Expressions evaluator method
 *
 * @param env Environment instance
 * @return Computed expression value
 * @see DrExpr
 */
fun DrExpr.eval(env: DrEnv): DrValue {
    return when (this) {
        // Literal
        is Literal -> value

        // Variable access
        is Variable -> {
            val value = env.resolve(name)
                ?: env.resolveClass(name)
                ?: throw DriftRuntimeException("Undefined symbol: $name")

            validateValue(unwrap(value))
        }

        // Callable call
        is Call -> {
            val callee = unwrap(callee.eval(env))
            val arguments = args.map { it.name to validateValue(it.expr.eval(env)) }


            data class ArgumentRules(
                val allowPositional: Boolean,
                val allowNamed: Boolean)

            /**
             * Verify provided arguments following parameters'
             * structure and rules.
             *
             * @param parameters Callable parameters structure
             * @param arguments Provided arguments (values, optionally named)
             * @param rules Verification parameters rules
             */
            fun resolveArguments(
                parameters: List<FunctionParameter>,
                arguments: List<Pair<String?, DrValue>>,
                rules: ArgumentRules
            ): Map<String, DrValue> {

                if (arguments.size > parameters.size)
                    throw DriftRuntimeException("Too many arguments.")

                val result = mutableMapOf<String, DrValue>()
                var positionalIndex = 0
                var namedSeen = false

                for ((name, value) in arguments) {
                    if (name == null) {
                        if (!rules.allowPositional)
                            throw DriftRuntimeException("Positional arguments are not allowed.")

                        if (namedSeen)
                            throw DriftRuntimeException(
                                "Positional arguments must appear before named arguments.")

                        if (positionalIndex >= parameters.size)
                            throw DriftRuntimeException("Too many positional arguments.")

                        val param = parameters[positionalIndex++]

                        if (param.name in result)
                            throw DriftRuntimeException(
                                "Parameter '${param.name}' is already bound.")

                        result[param.name] = value
                    } else {
                        if (!rules.allowNamed)
                            throw DriftRuntimeException("Named arguments are not allowed.")

                        namedSeen = true

                        val param = parameters.find { it.name == name }
                            ?: throw DriftRuntimeException("Unknown argument '$name'.")

                        if (param.name in result)
                            throw DriftRuntimeException(
                                "Parameter '$name' is already bound.")

                        result[param.name] = value
                    }
                }

                // Defaults & missing
                for (param in parameters) {
                    if (param.name !in result) {
                        val default = param.defaultValue
                            ?: throw DriftRuntimeException(
                                "Missing argument '${param.name}'.")

                        result[param.name] = validateValue(default.eval(DrEnv()))
                    }
                }

                return result
            }


            fun applyFunction(
                fn: Function,
                closure: DrEnv,
                boundArgs: Map<String, DrValue>,
                instance: DrInstance? = null
            ) {
                for (param in fn.parameters) {

                    val rawValue = boundArgs[param.name]
                        ?: throw DriftRuntimeException(
                            "Missing argument '${param.name}'")

                    val value = castNumericIfNeeded(rawValue, param.type)

                    if (param.type !is AnyType && !isAssignable(value.type(), param.type)) {
                        throw DriftTypeException(
                            "Invalid argument for '${param.name}'")
                    }

                    closure.define(param.name, value)
                }
            }


            fun evalFunction(
                function: Function,
                env: DrEnv,
                boundArgs: Map<String, DrValue>,
                instance: DrInstance? = null): DrValue {

                val newEnv = DrEnv(parent = env.copy())

                applyFunction(function, newEnv, boundArgs, instance)

                return evalBlock(function.returnType, function.body, newEnv)
            }


            when (callee) {
                is DrFunction -> {

                    val bindings = resolveArguments(
                        callee.let.parameters,
                        arguments,
                        ArgumentRules(
                            allowPositional = true,
                            allowNamed = true))

                    return evalFunction(
                        callee.let,
                        DrEnv(parent = callee.closure.copy()),
                        bindings)
                }
                is DrMethod -> {

                    val bindings = resolveArguments(
                        callee.let.parameters,
                        arguments,
                        ArgumentRules(
                            allowPositional = true,
                            allowNamed = true))

                    if (callee.nativeImpl != null)
                        return callee.nativeImpl.impl(callee.instance, arguments)

                    val newEnv = DrEnv(parent = callee.closure.copy()).apply {
                        if (callee.instance is DrInstance)
                            define("\$this", callee.instance)
                    }

                    return evalFunction(
                        callee.let,
                        newEnv,
                        bindings,
                        callee.instance as? DrInstance)
                }
                is DrNativeFunction -> {
                    return callee.impl(null, arguments)
                }
                is DrClass -> {

                    val constructor = callee.constructor
                    val expectedParametersCount = constructor?.let?.parameters?.size ?: 0
                    val actualParametersCount = arguments.size

                    if (arguments.size != expectedParametersCount) {
                        throw DriftRuntimeException(
                            "Wrong number of arguments for class '${callee.name}'\n" +
                            "Expected: $expectedParametersCount\n" +
                            "Actual: $actualParametersCount")
                    }

                    val initEnv = DrEnv(parent = callee.closure.copy())
                    val instanceEnv = DrEnv()

                    for ((name, field) in callee.fields) {
                        val variable = DrVariable(
                            name = name,
                            type = field.type,
                            value = DrNotAssigned,
                            isMutable = field.isMutable)

                        initEnv.define(name, variable)
                        instanceEnv.define(name, variable)
                    }

                    if (callee.constructorType == DrClass.ConstructorType.PRIMARY &&
                        constructor != null) {

                        val bindings = resolveArguments(
                            constructor.let.parameters,
                            arguments,
                            ArgumentRules(
                                allowPositional = false,
                                allowNamed = true))

                        constructor.let.parameters.forEach { param ->
                            val argValue = bindings[param.name]
                                ?: throw DriftRuntimeException("Missing argument '${param.name}'")

                            val variable = instanceEnv.resolve(param.name) as DrVariable
                            variable.set(castNumericIfNeeded(argValue, param.type))
                        }
                    }

                    for ((name, field) in callee.fields) {
                        val variable = instanceEnv.resolve(name) as DrVariable

                        if (variable.value == DrNotAssigned) {
                            val value = validateValue(field.value.eval(initEnv))
                            variable.set(value)
                        }
                    }

                    val instance = DrInstance(callee, instanceEnv)

                    if (callee.constructorType == DrClass.ConstructorType.STANDARD &&
                        constructor != null) {

                        val bindings = resolveArguments(
                            constructor.let.parameters,
                            arguments,
                            ArgumentRules(
                                allowPositional = true,
                                allowNamed = true))

                        val constructorEnv = DrEnv(parent = instanceEnv.copy()).apply {
                            define("\$this", instance)
                        }

                        evalFunction(constructor.let, constructorEnv, bindings, instance)
                    }

                    return instance
                }
                is DrLambda -> {

                    val bindings = resolveArguments(
                        callee.let.parameters,
                        arguments,
                        ArgumentRules(
                            allowPositional = true,
                            allowNamed = true))

                    val newEnv = DrEnv(callee.closure).apply {
                        callee.captures.forEach { (name, value) ->
                            forceDefine(name, value)
                        }
                    }

                    applyFunction(callee.let, newEnv, bindings)

                    return evalBlock(callee.let.returnType, callee.let.body, newEnv, true)
                }
                else -> throw DriftRuntimeException("Cannot call non-function: ${callee.asString()}")
            }
        }

        // Binary computing
        is Binary -> {
            fun unwrap(v: DrValue) : DrValue =
                if (v is DrVariable) v.value else v

            val leftValue = unwrap(left.eval(env))
            val rightValue = unwrap(right.eval(env))

            fun unsupportedOperator(op: String, leftType: DrType, rightType: DrType) : String =
                "Unsupported operator '$op' for types ${leftType.asString()} and ${rightType.asString()}"

            return when (operator) {
                "+" -> {
                    if (leftValue is DrNumeric && rightValue is DrNumeric) {
                        val (a, b, resultType) = promoteNumericPair(leftValue, rightValue)

                        return when (resultType) {
                            DrInt64::class -> DrInt64(a.asLong() + b.asLong())
                            DrUInt::class -> DrUInt(a.asUInt() + b.asUInt())
                            DrInt::class -> DrInt(a.asInt() + b.asInt())
                            else -> throw DriftRuntimeException(unsupportedOperator(
                                "+", leftValue.type(), rightValue.type()))
                        }
                    } else if (leftValue is DrString) {
                        return DrString(leftValue.value + rightValue.asString())
                    } else {
                        throw DriftRuntimeException(unsupportedOperator(
                            "+", leftValue.type(), rightValue.type()))
                    }
                }
                "-" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value - rightValue.value)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "-", leftValue.type(), rightValue.type()))
                    }
                }
                "*" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value * rightValue.value)
                        leftValue is DrString && rightValue is DrInt ->
                            DrString(leftValue.value.repeat(rightValue.value))
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "*", leftValue.type(), rightValue.type()))
                    }
                }
                "/" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt -> {
                            if (rightValue.value == 0) {
                                throw DriftRuntimeException("Division by zero is not allowed")
                            }

                            DrInt(leftValue.value / rightValue.value)
                        }
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "/", leftValue.type(), rightValue.type()))
                    }
                }
                "==" -> DrBool(leftValue == rightValue)
                "!=" -> DrBool(leftValue != rightValue)
                ">" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value > rightValue.value)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            ">", leftValue.type(), rightValue.type()))
                    }
                }
                "<" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value < rightValue.value)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "<", leftValue.type(), rightValue.type()))
                    }
                }
                ">=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value >= rightValue.value)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            ">=", leftValue.type(), rightValue.type()))
                    }
                }
                "<=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value <= rightValue.value)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "<=", leftValue.type(), rightValue.type()))
                    }
                }
                "><" -> when {
                    leftValue is DrNumeric && rightValue is DrRange -> {
                        val (l1, s1, _) = promoteNumericPair(leftValue, rightValue.from)
                        val (l2, e2, _) = promoteNumericPair(leftValue, rightValue.to)

                        DrBool(l1.asLong() >= s1.asLong() && l2.asLong() <= e2.asLong())
                    }
                    else -> throw DriftRuntimeException(unsupportedOperator(
                        "><", leftValue.type(), rightValue.type()))
                }
                ".." -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInclusiveRange(leftValue, rightValue)
                        leftValue is DrInt64 && rightValue is DrInt64 ->
                            DrInclusiveRange(leftValue, rightValue)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "..", leftValue.type(), rightValue.type()))
                    }
                }
                "..<" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrExclusiveRange(leftValue, rightValue)
                        leftValue is DrInt64 && rightValue is DrInt64 ->
                            DrExclusiveRange(leftValue, rightValue)
                        else -> throw DriftRuntimeException(unsupportedOperator(
                            "..<", leftValue.type(), rightValue.type()))
                    }
                }
                "&&" -> when {
                    leftValue is DrBool && rightValue is DrBool ->
                        DrBool(leftValue.value && rightValue.value)
                    else -> throw DriftRuntimeException(unsupportedOperator(
                        "&&", leftValue.type(), rightValue.type()))
                }
                "||" -> when {
                    leftValue is DrBool && rightValue is DrBool ->
                        DrBool(leftValue.value || rightValue.value)
                    else -> throw DriftRuntimeException(unsupportedOperator(
                        "||", leftValue.type(), rightValue.type()))
                }
                else -> throw DriftRuntimeException("Unknown binary operator '$operator'")
            }
        }

        // Conditional computing
        is Conditional -> {
            return if (evalCondition(condition, env)) {
                thenBranch.eval(env)
            } else {
                elseBranch?.eval(env) ?: DrNull
            }
        }

        // Lambda computing
        is Lambda -> {
            val f = Function("", this.parameters, this.body, this.returnType)
            val capture = env.all()
                .filterKeys { it != this.name }
                .mapValues { (_, v) -> unwrap(v) }
                .toMap()

            DrLambda(f, env.copy(), capture)
        }

        // Unary computing
        is Unary -> {
            val value = expr.eval(env)

            return when (operator) {
                "!" -> {
                    if (value !is DrBool)
                        throw DriftRuntimeException(
                            "Cannot negate non-boolean: ${value.type()}")

                    DrBool(!value.value)
                }
                "-" -> {
                    if (value !is DrInt)
                        throw DriftRuntimeException(
                            "Cannot negate non-integer: ${value.type()}")

                    DrInt(-value.value)
                }
                else -> throw DriftRuntimeException("Unknown unary operator '$operator'")
            }
        }

        // Variable assignment
        is Assign -> {
            val v = validateValue(value.eval(env))
            env.assign(name, v)
            DrVoid
        }

        // Object field getter
        is Get -> {

            val receiverValue = unwrap(receiver.eval(env))

            when (receiverValue) {
                is DrModule -> {
                    val value = receiverValue.get(name)
                        ?: throw DriftRuntimeException(
                            "Module symbol '${receiverValue.name}.$name' does not exist")

                    validateValue(unwrap(value))

                    return value
                }

                is DrInstance -> {
                    val klass = receiverValue.klass

                    // Instance Fields
                    if (receiverValue.has(name)) {
                        val value: DrValue = receiverValue.get(name)
                        validateValue(unwrap(value))

                        return value
                    }

                    // Instance Methods
                    klass.methods[name]?.let { method ->
                        return DrMethod(
                            let = method.let,
                            closure = env,
                            instance = receiverValue,
                            nativeImpl = method.nativeImpl
                        )
                    }

                    throw DriftRuntimeException("Member '$name' not found on class ${klass.name}")
                }

                is DrClass -> {
                    val klass = receiverValue

                    // Static fields
                    klass.staticFields[name]?.let { staticField ->
                        val variable = staticField.get(DrEnv(parent = receiverValue.closure))

                        validateValue(variable.value)

                        return variable.value
                    }

                    // Static methods
                    klass.staticMethods[name]?.let { staticMethod ->
                        return DrMethod(
                            let = staticMethod.let,
                            closure = env,
                            instance = null,
                            nativeImpl = staticMethod.nativeImpl
                        )
                    }

                    throw DriftRuntimeException("Static member '$name' not found on class ${klass.name}")
                }
            }

            val type = receiverValue.type()

            if (type is ObjectType) {
                val klass = env.resolveClass(type.className)
                    ?: throw DriftRuntimeException("'${type.className}' is not an object.")

                val method = klass.methods[name]
                    ?: throw DriftRuntimeException("'${type.className}.$name' method not found.")

                return DrMethod(
                    let = method.let,
                    closure = env,
                    instance = receiverValue,
                    nativeImpl = method.nativeImpl)
            }

            throw DriftRuntimeException("Cannot access attribute '$name' on non-object value")
        }

        // Object field setter
        is Set -> {
            val obj = unwrap(receiver.eval(env))
            val v = validateValue(value.eval(env))

            when (obj) {
                is DrModule -> throw DriftRuntimeException(
                    "Cannot assign to symbol '$name' in module ${obj.name}")

                is DrInstance -> obj.set(name, v)

                is DrClass -> {
                    val field = obj.staticFields[name]
                        ?: throw DriftRuntimeException("No static '$name' in class ${obj.name}")

                    field.set(env, v)
                }

                else -> throw DriftRuntimeException(
                    "Cannot set an attribute to a non-object value")
            }

            DrVoid
        }

        // List
        is ListLiteral -> {
            return DrList(values
                .map { unwrap(it.eval(env)) }
                .toMutableList())
        }

        else -> throw DriftRuntimeException("Invalid expression $this")
    }
}


private fun evalBlock(returnType: DrType, statements: List<DrStmt>, env: DrEnv, implicitLastAsReturnByDefault: Boolean = false) : DrValue {
    var last: DrValue = DrNull

    for (stmt in statements) {
        val result = stmt.eval(env)

        if (result is DrReturn) {
            if (!isAssignable(result.type(), returnType)) {
                throw DriftTypeException(
                    "Invalid return type: expected ${returnType.asString()}, " +
                    "got ${result.type().asString()}")
            }

            return unwrap(result.value)
        }

        last = result
    }

    return when {
        returnType == LastType -> unwrap(last)
        implicitLastAsReturnByDefault -> {
            val v = unwrap(last)

            if (isAssignable(v.type(), returnType)) {
                v
            } else {
                throw DriftTypeException(
                    "Invalid return type: expected ${returnType.asString()}, " +
                    "got ${v.type().asString()}")
            }
        }
        returnType == VoidType -> DrVoid
        returnType == AnyType  -> DrVoid
        else     -> throw DriftRuntimeException("Missing return statement")
    }
}