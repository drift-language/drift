/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.evaluators

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.Func
import drift.ast.bindings.FunctionParameter
import drift.ast.statements.Block
import drift.helper.evalCondition
import drift.helper.unwrap
import drift.helper.validateValue
import drift.runtime.*
import drift.runtime.exceptions.DMLNotFoundInModuleException
import drift.runtime.exceptions.DRArgumentAlreadyBoundException
import drift.runtime.exceptions.DRCannotNegateException
import drift.runtime.exceptions.DRCannotSetObjectException
import drift.runtime.exceptions.DRCannotSetViaModuleAccessException
import drift.runtime.exceptions.DRDivisionByZeroException
import drift.runtime.exceptions.DRInvalidExpressionException
import drift.runtime.exceptions.DRMissingArgumentException
import drift.runtime.exceptions.DRMissingReturnStatementException
import drift.runtime.exceptions.DRNamedArgumentsNotAllowedException
import drift.runtime.exceptions.DRNonCallableInvocationException
import drift.runtime.exceptions.DRNotAnObjectException
import drift.runtime.exceptions.DRPositionalArgumentsNotAllowedException
import drift.runtime.exceptions.DRPositionalMustPrecedeNamedArgumentsException
import drift.runtime.exceptions.DRTooManyArgumentsException
import drift.runtime.exceptions.DRTooManyPositionalArgumentsException
import drift.runtime.exceptions.DRUnknownClassMemberException
import drift.runtime.exceptions.DRUnknownClassStaticMemberException
import drift.runtime.exceptions.DRUnknownOperatorException
import drift.runtime.exceptions.DRUnknownParameterException
import drift.runtime.exceptions.DRUnsuccessfulCastException
import drift.runtime.exceptions.DRUnsupportedOperatorException
import drift.runtime.exceptions.DRVariableNotDefinedException
import drift.runtime.exceptions.DRWrongNumberOfClassArgumentsException
import drift.runtime.values.callables.*
import drift.runtime.values.containers.list.ParserArray
import drift.runtime.values.containers.range.ParserExclusiveRange
import drift.runtime.values.containers.range.ParserInclusiveRange
import drift.runtime.values.containers.range.ParserRange
import drift.runtime.values.imports.ParserModule
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.oop.ParserInstance
import drift.runtime.values.primaries.*
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserNull
import drift.runtime.values.specials.ParserVoid
import drift.runtime.values.variables.ParserVariable
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
 * @see ParserExpression
 */
fun ParserExpression.eval(env: DrEnv): ParserValue {
    return when (this) {
        // Literal
        is Literal -> value

        // Variable access
        is Variable -> {
            val value = env.resolve(name)
                ?: env.resolveClass(name)
                ?: throw DRVariableNotDefinedException(name = name)

            validateValue(unwrap(value))
            value
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
                arguments: List<Pair<String?, ParserValue>>,
                rules: ArgumentRules
            ): Map<String, ParserValue> {

                if (arguments.size > parameters.size)
                    throw DRTooManyArgumentsException(
                        expected = parameters.size,
                        actual = arguments.size)

                val result = mutableMapOf<String, ParserValue>()
                var positionalIndex = 0
                var namedSeen = false

                for ((name, value) in arguments) {
                    if (name == null) {
                        if (!rules.allowPositional)
                            throw DRPositionalArgumentsNotAllowedException()

                        if (namedSeen)
                            throw DRPositionalMustPrecedeNamedArgumentsException()

                        if (positionalIndex >= parameters.size)
                            throw DRTooManyPositionalArgumentsException(
                                expected = parameters.size,
                                actual = positionalIndex)

                        val param = parameters[positionalIndex++]

                        if (param.name in result)
                            throw DRArgumentAlreadyBoundException(name = param.name)

                        result[param.name] = value
                    } else {
                        if (!rules.allowNamed)
                            throw DRNamedArgumentsNotAllowedException()

                        namedSeen = true

                        val param = parameters.find { it.name == name }
                            ?: throw DRUnknownParameterException(name = name)

                        if (param.name in result)
                            throw DRArgumentAlreadyBoundException(name = name)

                        result[param.name] = value
                    }
                }

                // Defaults & missing
                for (param in parameters) if (param.name !in result) {
                    val default = param.defaultValue
                        ?: throw DRMissingArgumentException(name = param.name)

                    result[param.name] = validateValue(default.eval(DrEnv()))
                }

                return result
            }


            fun applyFunction(
                fn: Func,
                closure: DrEnv,
                boundArgs: Map<String, ParserValue>) {

                for (param in fn.parameters) {
                    val rawValue = boundArgs[param.name]
                        ?: throw DRMissingArgumentException(name = param.name)

                    val value = castNumericIfNeeded(rawValue, param.type)

                    if (param.type !is AnyType && !isAssignable(value.type(), param.type))
                        throw DRUnsuccessfulCastException(
                            valueType = value.type(),
                            expectedType = param.type)

                    closure.define(param.name, value)
                }
            }


            fun evalFunction(
                func: Func,
                env: DrEnv,
                boundArgs: Map<String, ParserValue>): ParserValue {

                val newEnv = DrEnv(parent = env.copy())

                applyFunction(func, newEnv, boundArgs)

                return evalBlock(func.returnType, func.body, newEnv)
            }


            when (callee) {
                is ParserFunction -> {

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

                is ParserMethod -> {

                    val bindings = resolveArguments(
                        callee.let.parameters,
                        arguments,
                        ArgumentRules(
                            allowPositional = true,
                            allowNamed = true))

                    if (callee.nativeImpl != null)
                        return callee.nativeImpl.impl(callee.instance, arguments)

                    val newEnv = DrEnv(parent = callee.closure.copy()).apply {
                        if (callee.instance is ParserInstance)
                            define("\$this", callee.instance)
                    }

                    return evalFunction(
                        callee.let,
                        newEnv,
                        bindings)
                }

                is ParserNativeFunction ->
                    callee.impl(null, arguments)

                is ParserClass -> {

                    val constructor = callee.constructor
                    val expectedParametersCount = constructor?.let?.parameters?.size ?: 0
                    val actualParametersCount = arguments.size

                    if (arguments.size != expectedParametersCount)
                        throw DRWrongNumberOfClassArgumentsException(
                            className = callee.name,
                            expected = expectedParametersCount,
                            actual = actualParametersCount)

                    val callEnv = DrEnv(parent = callee.closure.copy())
                    val instanceEnv = DrEnv()

                    for ((name, field) in callee.fields) {
                        val variable = ParserVariable(
                            name = name,
                            type = field.type,
                            value = ParserNotAssigned,
                            isMutable = field.isMutable)

                        callEnv.define(name, variable)
                        instanceEnv.define(name, variable)
                    }

                    if (callee.constructorType == ParserClass.ConstructorType.PRIMARY &&
                        constructor != null) {

                        val bindings = resolveArguments(
                            constructor.let.parameters,
                            arguments,
                            ArgumentRules(
                                allowPositional = false,
                                allowNamed = true))

                        constructor.let.parameters.forEach { param ->

                            val argValue = bindings[param.name]
                                ?: throw DRMissingArgumentException(name = param.name)

                            val variable = instanceEnv.resolve(param.name) as ParserVariable
                            variable.set(castNumericIfNeeded(argValue, param.type))
                        }
                    }

                    for ((name, field) in callee.fields) {
                        val variable = instanceEnv.resolve(name) as ParserVariable

                        if (variable.value == ParserNotAssigned) {
                            val value = validateValue(field.value.eval(callEnv))
                            variable.set(value)
                        }
                    }

                    val instance = ParserInstance(callee, instanceEnv)

                    if (callee.constructorType == ParserClass.ConstructorType.STANDARD &&
                        constructor != null) {

                        val bindings = resolveArguments(
                            constructor.let.parameters,
                            arguments,
                            ArgumentRules(
                                allowPositional = true,
                                allowNamed = true))

                        val constructorEnv = DrEnv(parent = callee.closure.copy()).apply {
                            define("\$this", instance)
                        }

                        evalFunction(
                            constructor.let,
                            constructorEnv,
                            bindings)
                    }

                    return instance
                }

                is ParserLambda -> {

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
                else -> throw DRNonCallableInvocationException(
                    name = callee.asString())
            }
        }

        // Binary computing
        is Binary -> {

            fun unwrap(v: ParserValue) : ParserValue =
                if (v is ParserVariable) v.value else v

            val leftValue = unwrap(left.eval(env))
            val rightValue = unwrap(right.eval(env))

            return when (operator) {
                "+" -> {
                    if (leftValue is DrNumeric && rightValue is DrNumeric) {
                        val (a, b, resultType) = promoteNumericPair(leftValue, rightValue)

                        return when (resultType) {
                            ParserInt64::class -> ParserInt64(a.asLong() + b.asLong())
                            ParserUInt::class -> ParserUInt(a.asUInt() + b.asUInt())
                            ParserInt::class -> ParserInt(a.asInt() + b.asInt())
                            else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                        }
                    } else if (leftValue is ParserString) {
                        return ParserString(leftValue.value + rightValue.asString())
                    } else {
                        throw DRUnsupportedOperatorException(
                            operator = operator,
                            types = leftValue.type() to rightValue.type())
                    }
                }
                "-" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserInt(leftValue.value - rightValue.value)
                        else -> throw DRUnsupportedOperatorException(
                            operator = operator,
                            types = leftValue.type() to rightValue.type())
                    }
                }
                "*" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserInt(leftValue.value * rightValue.value)
                        leftValue is ParserString && rightValue is ParserInt ->
                            ParserString(leftValue.value.repeat(rightValue.value))
                        else -> throw DRUnsupportedOperatorException(
                            operator = operator,
                            types = leftValue.type() to rightValue.type())
                    }
                }
                "/" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt -> {
                            if (rightValue.value == 0)
                                throw DRDivisionByZeroException()

                            ParserInt(leftValue.value / rightValue.value)
                        }
                        else -> throw DRUnsupportedOperatorException(
                            operator = operator,
                            types = leftValue.type() to rightValue.type())
                    }
                }
                "==" -> ParserBool(leftValue == rightValue)
                "!=" -> ParserBool(leftValue != rightValue)
                ">" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserBool(leftValue.value > rightValue.value)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                "<" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserBool(leftValue.value < rightValue.value)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                ">=" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserBool(leftValue.value >= rightValue.value)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                "<=" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserBool(leftValue.value <= rightValue.value)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                "><" -> when {
                    leftValue is DrNumeric && rightValue is ParserRange -> {
                        val (l1, s1, _) = promoteNumericPair(leftValue, rightValue.from)
                        val (l2, e2, _) = promoteNumericPair(leftValue, rightValue.to)

                        ParserBool(l1.asLong() >= s1.asLong() && l2.asLong() <= e2.asLong())
                    }
                    else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                }
                ".." -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserInclusiveRange(leftValue, rightValue)
                        leftValue is ParserInt64 && rightValue is ParserInt64 ->
                            ParserInclusiveRange(leftValue, rightValue)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                "..<" -> {
                    when {
                        leftValue is ParserInt && rightValue is ParserInt ->
                            ParserExclusiveRange(leftValue, rightValue)
                        leftValue is ParserInt64 && rightValue is ParserInt64 ->
                            ParserExclusiveRange(leftValue, rightValue)
                        else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                    }
                }
                "&&" -> when {
                    leftValue is ParserBool && rightValue is ParserBool ->
                        ParserBool(leftValue.value && rightValue.value)
                    else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                }
                "||" -> when {
                    leftValue is ParserBool && rightValue is ParserBool ->
                        ParserBool(leftValue.value || rightValue.value)
                    else -> throw DRUnsupportedOperatorException(
                                operator = operator,
                                types = leftValue.type() to rightValue.type())
                }
                else -> throw DRUnknownOperatorException(operator = operator)
            }
        }

        // Conditional computing
        is Conditional -> {
            return if (evalCondition(condition, env)) {
                thenBranch.eval(env)
            } else {
                elseBranch?.eval(env) ?: ParserNull
            }
        }

        // Lambda computing
        is Lambda -> {
            val f = Func(name = "", parameters = this.parameters, body = this.body, returnType = this.returnType)
            val capture = env.all()
                .mapValues { (_, v) -> unwrap(v) }
                .toMap()

            ParserLambda(f, env.copy(), capture)
        }

        // Unary computing
        is Unary -> {
            val value = expr.eval(env)

            return when (operator) {
                "!" -> {
                    if (value !is ParserBool)
                        throw DRCannotNegateException(
                            type = value.type(),
                            operator = operator)

                    ParserBool(!value.value)
                }
                "-" -> {
                    if (value !is ParserInt)
                        throw DRCannotNegateException(
                            type = value.type(),
                            operator = operator)

                    ParserInt(-value.value)
                }
                else -> throw DRUnknownOperatorException(operator = operator)
            }
        }

        // Variable assignment
        is Assign -> {
            val v = validateValue(value.eval(env))
            env.assign(name, v)
            ParserVoid
        }

        // Object field getter
        is Get -> {

            val receiverValue = unwrap(receiver.eval(env))

            when (receiverValue) {
                is ParserModule -> {
                    val value = receiverValue.get(name)
                        ?: throw DMLNotFoundInModuleException(
                            element = name,
                            moduleNamespace = receiverValue.name)

                    validateValue(unwrap(value))

                    return value
                }

                is ParserInstance -> {
                    val klass = receiverValue.klass

                    // Instance Fields
                    if (receiverValue.has(name)) {
                        val value: ParserValue = receiverValue.get(name)
                        validateValue(unwrap(value))

                        return value
                    }

                    // Instance Methods
                    klass.methods[name]?.let { method ->
                        return ParserMethod(
                            let = method.let,
                            closure = klass.closure.copy(),
                            instance = receiverValue,
                            nativeImpl = method.nativeImpl
                        )
                    }

                    throw DRUnknownClassMemberException(
                        memberName = name,
                        className = klass.name)
                }

                is ParserClass -> {
                    val klass = receiverValue

                    // Static fields
                    klass.staticFields[name]?.let { staticField ->
                        val variable = staticField.get(DrEnv(parent = receiverValue.closure))

                        validateValue(variable.value)

                        return variable.value
                    }

                    // Static methods
                    klass.staticMethods[name]?.let { staticMethod ->
                        return ParserMethod(
                            let = staticMethod.let,
                            closure = env,
                            instance = null,
                            nativeImpl = staticMethod.nativeImpl
                        )
                    }

                    throw DRUnknownClassStaticMemberException(
                        memberName = name,
                        className = klass.name)
                }
            }

            val type = receiverValue.type()

            if (type is ObjectType) {
                val klass = env.resolveClass(type.className)
                    ?: throw DRNotAnObjectException(valueType = type)

                val method = klass.methods[name]
                    ?: throw DRUnknownClassMemberException(
                        memberName = name,
                        className = type.className)

                return ParserMethod(
                    let = method.let,
                    closure = klass.closure.copy(),
                    instance = receiverValue,
                    nativeImpl = method.nativeImpl)
            }

            throw DRNotAnObjectException(valueType = type)
        }

        // Object field setter
        is Set -> {
            val obj = unwrap(receiver.eval(env))
            val v = validateValue(value.eval(env))

            val objType = obj.type()
            if (objType !is ObjectType && objType !is ClassType)
                throw DRNotAnObjectException(valueType = objType)

            when (obj) {
                is ParserModule -> throw DRCannotSetViaModuleAccessException()

                is ParserInstance -> obj.set(name, v)

                is ParserClass -> {
                    val field = obj.staticFields[name]
                        ?: throw DRUnknownClassStaticMemberException(
                            memberName = name,
                            className = obj.name)

                    field.set(env, v)
                }

                else -> throw DRCannotSetObjectException(
                    type = obj.type())
            }

            ParserVoid
        }

        // List
        is drift.ast.expressions.Array -> ParserArray(values
            .map { unwrap(it.eval(env)) })

        else -> throw DRInvalidExpressionException()
    }
}


private fun evalBlock(returnType: ParserType, block: Block, env: DrEnv, implicitLastAsReturnByDefault: Boolean = false) : ParserValue {
    var last: ParserValue = ParserNull

    for (stmt in block.statements) {
        val result = stmt.eval(env)

        if (result is ParserReturn) {
            if (!isAssignable(result.type(), returnType))
                throw DRUnsuccessfulCastException(
                    valueType = result.type(),
                    expectedType = returnType)

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
                throw DRUnsuccessfulCastException(
                    valueType = v.type(),
                    expectedType = returnType)
            }
        }
        returnType == VoidType || returnType == AnyType -> ParserVoid
        else -> throw DRMissingReturnStatementException()
    }
}