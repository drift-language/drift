package drift.ast

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
                                ?: error("Missing positional argument for '${param.name}'")

                            arg.second
                        } else {
                            val arg = arguments.find { it.first == param.name }
                                ?: error("Missing positional argument for '${param.name}'")

                            arg.second
                        }

                        newEnv.define(param.name, value)
                    }

                    return evalBlock(callee.body, newEnv)
                }
                is DrNativeFunction -> callee.impl(arguments)
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
                        else -> error("Unsupported '+' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "-" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value - rightValue.value)
                        else -> error("Unsupported '-' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "*" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrInt(leftValue.value * rightValue.value)
                        leftValue is DrString && rightValue is DrInt ->
                            DrString(leftValue.value.repeat(rightValue.value))
                        else -> error("Unsupported '*' for types ${leftValue::class} and ${rightValue::class}")
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
                        else -> error("Unsupported '>' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "<" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value < rightValue.value)
                        else -> error("Unsupported '<' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                ">=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value >= rightValue.value)
                        else -> error("Unsupported '>=' for types ${leftValue::class} and ${rightValue::class}")
                    }
                }
                "<=" -> {
                    when {
                        leftValue is DrInt && rightValue is DrInt ->
                            DrBool(leftValue.value <= rightValue.value)
                        else -> error("Unsupported '<=' for types ${leftValue::class} and ${rightValue::class}")
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
    }
}

private fun evalBlock(statements: List<DrStmt>, env: DrEnv) : DrValue {
    try {
        var last: DrValue = DrNull

        for (stmt in statements) {
            last = stmt.eval(env)
        }

        return last
    } catch (e: ReturnException) {
        return e.value
    }
}