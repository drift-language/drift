/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.evaluators

import drift.ast.Assign
import drift.ast.Binary
import drift.ast.Call
import drift.ast.Conditional
import drift.ast.DrExpr
import drift.ast.DrStmt
import drift.ast.Function
import drift.ast.Get
import drift.ast.Lambda
import drift.ast.ListLiteral
import drift.ast.Literal
import drift.ast.Set
import drift.ast.Unary
import drift.ast.Variable
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
import drift.runtime.values.containers.DrExclusiveRange
import drift.runtime.values.containers.DrInclusiveRange
import drift.runtime.values.containers.list.DrList
import drift.runtime.values.containers.DrRange
import drift.runtime.values.oop.DrClass
import drift.runtime.values.oop.DrInstance
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrNumeric
import drift.runtime.values.primaries.DrString
import drift.runtime.values.primaries.DrUInt
import drift.runtime.values.primaries.promoteNumericPair
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
 * @see drift.ast.DrExpr
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

            fun applyFunction(
                fn: Function,
                closure: DrEnv,
                args: List<Pair<String?, DrValue>>,
                instance: DrInstance? = null) {

                /*
                    TODO:
                    - verify position for positional and named arguments
                            NAMED… -> POS…
                 */

                for ((index, param) in fn.parameters.withIndex()) {
                    var value = if (param.isPositional) {
                        val arg = arguments.getOrNull(index)

                        arg?.second
                            ?: when (val v = param.defaultValue?.eval(env)) {
                                is DrValue -> arguments.firstOrNull { it.first == param.name }?.second
                                    ?: unwrap(v)
                                else -> throw DriftRuntimeException(
                                    "Missing positional argument for '${param.name}'")
                            }
                    } else {
                        val arg = arguments.find { it.first == param.name }

                        arg?.second
                            ?: when (val v = param.defaultValue?.eval(env)) {
                                is DrValue -> {
                                    arguments.firstOrNull { it.first == param.name }?.second
                                        ?: unwrap(v)
                                }
                                else -> throw DriftRuntimeException(
                                    "Missing argument for '${param.name}'")
                            }
                    }

                    value = castNumericIfNeeded(value, param.type)

                    if (param.type !is AnyType && !isAssignable(value.type(), param.type)) {
                        throw DriftTypeException("Invalid argument for '${param.name}'")
                    }

                    closure.define(param.name, value)
                }
            }

            when (callee) {
                is DrFunction -> {
                    val newEnv = DrEnv(parent = callee.closure.copy())

                    applyFunction(callee.let, newEnv, arguments)

                    return evalBlock(callee.let.returnType, callee.let.body, newEnv)
                }
                is DrMethod -> {
                    if (callee.nativeImpl != null) {
                        return callee.nativeImpl.impl(callee.instance, arguments)
                    }

                    val newEnv = DrEnv(parent = callee.closure.copy()).apply {
                        if (callee.instance is DrInstance) {
                            define("this", callee.instance)
                        }
                    }

                    applyFunction(callee.let, newEnv, arguments, callee.instance as? DrInstance)

                    return evalBlock(callee.let.returnType, callee.let.body, newEnv)
                }
                is DrNativeFunction -> callee.impl(null, arguments)
                is DrClass -> {
                    if (arguments.size != callee.fields.size) {
                        throw DriftRuntimeException("Wrong number of arguments for class '${callee.name}'")
                    }

                    val valueMap = mutableMapOf<String, DrValue>()

                    for ((index, field) in callee.fields.withIndex()) {
                        val value: DrValue = arguments[index].second

                        if (!isAssignable(value.type(), field.type)) {
                            throw DriftTypeException(
                                "Field '${field.name}' of '${callee.name}' " +
                                "expects ${field.type}, got ${value.type()}")
                        }

                        valueMap[field.name] = value
                    }

                    return DrInstance(callee, valueMap)
                }
                is DrLambda -> {
                    val newEnv = DrEnv(callee.closure).apply {
                        callee.captures.forEach { (name, value) ->
                            forceDefine(name, value)
                        }
                    }

                    applyFunction(callee.let, newEnv, arguments)

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
            v
        }

        // Object field getter
        is Get -> {
            val obj = unwrap(receiver.eval(env))

            val klass = env.resolveClass(obj.type().asString())
                ?: throw DriftRuntimeException("No class found for ${obj.type().asString()}")

            when (obj) {
                is DrInstance -> {
                    // Instance Fields
                    obj.values[name]?.let { value ->
                        validateValue(value)
                        return value
                    }

                    // Instance Methods
                    klass.methods.find { it.let.name == name }?.let { method ->
                        return DrMethod(
                            let = method.let,
                            closure = env,
                            instance = obj,
                            nativeImpl = method.nativeImpl
                        )
                    }
                }
                is DrClass -> {
                    // Static fields
                    klass.staticFields[name]?.let { staticField ->
                        validateValue(staticField.value)
                        return staticField.value
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
                }
                else -> throw DriftRuntimeException("Cannot set an attribute to a non-object value")
            }

            throw DriftRuntimeException("Property or method '$name' not found on class ${klass.name}")
        }

        // Object field setter
        is Set -> {
            val obj = receiver.eval(env)

            val v = value.eval(env)

            when (obj) {
                is DrInstance -> obj.set(name, v)
                is DrClass -> {
                    val field = obj.staticFields[name]
                        ?: throw DriftRuntimeException("No static '$name' in class ${obj.name}")

                    field.value = v
                }
                else -> throw DriftRuntimeException("Cannot set an attribute to a non-object value")
            }

            v
        }

        // List
        is ListLiteral -> {
            return DrList(values
                .map { unwrap(it.eval(env)) }
                .toMutableList())
        }
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