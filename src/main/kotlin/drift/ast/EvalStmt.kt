package drift.ast

import drift.exceptions.DriftRuntimeException
import drift.helper.unwrap
import drift.helper.validateValue
import drift.runtime.*

fun DrStmt.eval(env: DrEnv): DrValue {
    return when (this) {
        is ExprStmt -> expr.eval(env)
        is Block -> {
            val subEnv = DrEnv(env)
            var last: DrValue = DrNull

            for (statement in statements) {
                last = statement.eval(subEnv)

                if (last is DrReturn)
                    return last
            }

            last
        }
        is If -> {
            var result: DrValue = DrNull

            if (drift.helper.evalCondition(condition, env)) {
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
            val value = validateValue(value.eval(env))

            DrReturn(value)
        }
        is Class -> {
            val klass = DrClass(name, fields, methods.map {
                DrMethod(it, env)
            })

            if (env.isTopLevel()) {
                env.assignClass(name, klass)
            } else {
                env.defineClass(name, klass)
            }

            DrVoid
        }
        is Let -> {
            val v = validateValue(value.eval(env), ignoreNotAssigned = true)

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