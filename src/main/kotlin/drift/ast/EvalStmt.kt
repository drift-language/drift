package drift.ast

import drift.runtime.*

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
        is Function -> {
            val function = DrFunction(this, env.copy())
            env.define(name, function)
            function
        }
        is Return -> {
            val value = value.eval(env)
            throw ReturnException(value)
        }
        is Class -> {
            val klass = DrClass(name, fields, methods.map {
                DrMethod(it, env, null)
            })
            env.define(name, klass)
            DrVoid
        }
        is Let -> {
            val v = value.eval(env)

            env.define(name, DrVariable(name, type, v, isMutable))

            v
        }
    }
}