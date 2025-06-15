package drift.ast

import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.runtime.*

fun DrExpr.eval(env: DrEnv): DrValue {
    return when (this) {
        is Literal -> value
        is Variable -> env.get(name)
        is Call -> {
            val callee = callee.eval(env)
            val arguments = args.map { it.name to it.expr.eval(env) }

            when (callee) {
                is DrFunction -> {
                    val newEnv = DrEnv(parent = callee.closure)

                    for ((index, param) in callee.params.withIndex()) {
                        val value = if (param.isPositional) {
                            val arg = arguments.getOrNull(index)
                                ?: throw DriftRuntimeException("Missing positional argument for '${param.name}'")

                            arg.second
                        } else {
                            val arg = arguments.find { it.first == param.name }
                                ?: throw DriftRuntimeException("Missing positional argument for '${param.name}'")

                            arg.second
                        }

                        if (param.type !is AnyType && !isAssignable(value.type(), param.type)) {
                            throw DriftTypeException("Invalid argument for '${param.name}'")
                        }

                        newEnv.define(param.name, value)
                    }

                    return evalBlock(callee.returnType, callee.body, newEnv)
                }
                is DrNativeFunction -> callee.impl(arguments)
                is DrClass -> {
                    if (arguments.size != callee.fields.size) {
                        error("Wrong number of arguments for class '${callee.name}'")
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
                else -> error("Cannot call non-function: ${callee.asString()}")
            }
        }
        is Binary -> {
            val leftValue = left.eval(env)
            val rightValue = right.eval(env)

            return when (operator) {
                "+" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value + rightValue.value)
                        leftValue is DrString && rightValue is DrString ->
                            DrString(leftValue.value + rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '+' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "-" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value - rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '-' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "*" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value * rightValue.value)
                        leftValue is DrString && rightValue is DrInt ->
                            DrString(leftValue.value.repeat(rightValue.value))
                        else -> throw DriftRuntimeException(
                            "Unsupported '*' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "/" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt -> {
                            if (rightValue.value == 0) {
                                error("Division by zero is not allowed")
                            }

                            DrInt(leftValue.value / rightValue.value)
                        }
                        else -> error("Unsupported '/' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "==" -> DrBool(leftValue == rightValue)
                "!=" -> DrBool(leftValue != rightValue)
                ">" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value > rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '>' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "<" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value < rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '<' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                ">=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value >= rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '>=' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "<=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value <= rightValue.value)
                        else -> throw DriftRuntimeException(
                            "Unsupported '<=' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                else -> error("Unknown binary operator '$operator'")
            }
        }
        is Conditional -> {
            val conditionValue = condition.eval(env)

            if (conditionValue !is DrBool) error("Condition must be boolean")

            return if (conditionValue.value) {
                thenBranch.eval(env)
            } else {
                elseBranch?.eval(env) ?: DrNull
            }
        }
        is Lambda -> DrFunction(parameters, body, env.copy(), returnType)
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
    }
}

private fun evalBlock(returnType: DrType,statements: List<DrStmt>, env: DrEnv) : DrValue {
    try {
        var last: DrValue = DrNull
        var hasReturnStatement = false

        for (stmt in statements) {
            last = stmt.eval(env)

            if (stmt is Return) {
                hasReturnStatement = true
            }
        }

        if (!hasReturnStatement) {
            return when (returnType) {
                VoidType -> DrVoid
                AnyType -> DrVoid
                LastType -> last
                else     -> error("Missing return statement")
            }
        }

        if (!isAssignable(last.type(), returnType)) {
            throw DriftTypeException("Invalid return type: expected ${returnType.asString()}, got ${last.type().asString()}")
        }

        return last
    } catch (e: ReturnException) {
        if (!isAssignable(e.value.type(), returnType)) {
            throw DriftTypeException("Invalid return type: expected ${returnType.asString()}, got ${e.value.type().asString()}")
        }

        return e.value
    }
}