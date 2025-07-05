package drift.ast

import drift.exceptions.DriftRuntimeException
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
            val f = DrFunction(this, env.copy())

            if (env.isTopLevel()) {
                env.forceDefine(name, f)
            } else {
                env.define(name, f)
            }

            f
        }
        is Return -> {
            val value = value.eval(env)
            throw ReturnException(value)
        }
        is Class -> {
            val klass = DrClass(name, fields, methods.map {
                DrMethod(it, env, null)
            })

            if (env.isTopLevel()) {
                env.assignClass(name, klass)
            } else {
                env.defineClass(name, klass)
            }

            DrVoid
        }
        is Let -> {
            val v = value.eval(env)

            if (env.isTopLevel()) {
                val variable = env.resolve(name) as? DrVariable
                    ?: throw DriftRuntimeException("Variable $name not found in global scope")

                variable.set(v)
            } else {
                env.define(name, DrVariable(name, type, v, isMutable))
            }

            v
        }
    }
}