package drift.ast

import drift.runtime.DrBool
import drift.runtime.DrEnv
import drift.runtime.DrNull
import drift.runtime.DrValue

fun DrStmt.eval(env: DrEnv): DrValue {
    return when (this) {
        is ExprStmt -> expr.eval(env)
        is Block -> {
            val subEnv = DrEnv(parent = env)
            var last: DrValue = DrNull

            for (statement in statements) {
                last = statement.eval(subEnv)
            }

            last
        }
        is If -> {
            var result: DrValue = DrNull

            if (condition.eval(env) == DrBool(true)) {
                result = thenBranch.eval(env)
            } else if (elseBranch != null) {
                result = elseBranch.eval(env)
            }

            result
        }
    }
}