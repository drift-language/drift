package drift.ast

import drift.runtime.*

fun DrExpr.eval(env: DrEnv): DrValue {
    return when (this) {
        is Literal -> value
        is Variable -> env.get(name)
        is Call -> {
            val calleeValue = callee.eval(env)
            val argValues = args.map { it.eval(env) }

            if (calleeValue is DrFunction) {
                calleeValue.invoke(argValues)
            } else {
                error("Trying to call a non-function: ${calleeValue.asString()}")
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