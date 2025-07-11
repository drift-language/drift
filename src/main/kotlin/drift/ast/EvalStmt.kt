/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast

import drift.exceptions.DriftRuntimeException
import drift.helper.validateValue
import drift.runtime.*
import drift.runtime.values.callables.DrFunction
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrReturn
import drift.runtime.values.containers.DrList
import drift.runtime.values.containers.DrRange
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.specials.DrNull
import drift.runtime.values.specials.DrVoid
import drift.runtime.values.variables.DrVariable
import drift.utils.castNumericIfNeeded


/******************************************************************************
 * DRIFT STATEMENTS EVALUATOR
 *
 * This evaluator computes all Drift statements
 ******************************************************************************/



/**
 * Statements evaluator method
 *
 * @param env Environment instance
 * @return Computed statement value
 * @see DrStmt
 */
fun DrStmt.eval(env: DrEnv): DrValue {
    return when (this) {
        // Expression statement computing
        is ExprStmt -> expr.eval(env)

        // Block
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

        // Classic conditional computing
        is If -> {
            var result: DrValue = DrNull

            if (drift.helper.evalCondition(condition, env)) {
                result = thenBranch.eval(env)
            } else if (elseBranch != null) {
                result = elseBranch.eval(env)
            }

            result
        }

        // Function computing
        is Function -> {
            val f = DrFunction(this, env.copy())

            if (env.isTopLevel()) {
                env.forceDefine(name, f)
            } else {
                env.define(name, f)
            }

            f
        }

        // Callable return
        is Return -> {
            val value = validateValue(value.eval(env))

            DrReturn(value)
        }

        // Class definition
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

        // Variable definition
        is Let -> {
            var v = validateValue(value.eval(env), ignoreNotAssigned = true)

            if (type != AnyType) {
                v = castNumericIfNeeded(v, type)
            }

            if (env.isTopLevel()) {
                val variable = env.resolve(name) as? DrVariable
                    ?: throw DriftRuntimeException("Variable $name not found in global scope")

                variable.set(v)
            } else {
                env.define(name, DrVariable(name, type, v, isMutable))
            }

            v
        }

        // For loop computing
        is For -> {
            val iterable = iterable.eval(env)

            val items = when (iterable) {
                is DrList -> iterable.items
                is DrRange -> (iterable.from.value..iterable.to.value).map { DrInt(it) }
                else -> throw DriftRuntimeException("Cannot iterate over ${iterable.type()}")
            }

            for (item in items) {
                val loopEnv = DrEnv(env)

                if (variables.isEmpty()) {
                    loopEnv.forceDefine("_", item)
                } else if (variables.size == 1) {
                    val name = variables[0]

                    loopEnv.forceDefine(name, DrVariable(name, AnyType, item, isMutable = true))
                } else if (item is DrList && item.items.size == variables.size) {
                    variables.zip(item.items).forEach { (name, value) ->
                        loopEnv.assign(name, DrVariable(name, AnyType, value, isMutable = true))
                    }
                } else {
                    throw DriftRuntimeException(
                        "Cannot destructure ${item.type().asString()} into " +
                        "${variables.size} variables")
                }

                body.eval(loopEnv)
            }

            DrVoid
        }
    }
}