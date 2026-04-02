/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.evaluators

import drift.ast.statements.*
import drift.ast.statements.Func
import drift.helper.evalCondition
import drift.helper.rangeToList
import drift.helper.validateValue
import drift.runtime.*
import drift.runtime.exceptions.DRCannotDestructureException
import drift.runtime.exceptions.DRCannotIterateException
import drift.runtime.exceptions.DRInvalidStatementException
import drift.runtime.exceptions.DRVariableNotDefinedException
import drift.runtime.values.callables.ParserFunction
import drift.runtime.values.callables.ParserReturn
import drift.runtime.values.containers.list.ParserArray
import drift.runtime.values.containers.range.ParserExclusiveRange
import drift.runtime.values.containers.range.ParserInclusiveRange
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.specials.ParserNull
import drift.runtime.values.specials.ParserVoid
import drift.runtime.values.variables.ParserVariable
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
 * @see ParserStatement
 */
fun ParserStatement.eval(env: DrEnv): ParserValue {
    return when (this) {
        // Expression statement computing
        is ExprStmt -> expr.eval(env)

        // Block
        is Block -> {
            val subEnv = DrEnv(env)
            var last: ParserValue = ParserNull

            for (statement in statements) {
                last = statement.eval(subEnv)

                if (last is ParserReturn)
                    return last
            }

            last
        }

        // Classic conditional computing
        is If -> {
            var result: ParserValue = ParserNull

            if (evalCondition(condition, env)) {
                result = thenBranch.eval(env)
            } else if (elseBranch != null) {
                result = elseBranch.eval(env)
            }

            result
        }

        // Function computing
        is Func -> {
            val f = ParserFunction(this, env.copy())

            env.assign(name, f)

            f
        }

        // Callable return
        is Return -> {
            val value = validateValue(value.eval(env), ignoreVoid = true)

            ParserReturn(value)
        }

        // Class definition
        is Class -> ParserVoid      /* NOTE: defined by collector */

        // Variable definition
        is Let -> {
            var v = validateValue(value.eval(env), ignoreNotAssigned = true)

            if (type != AnyType) {
                v = castNumericIfNeeded(v, type)
            }

            if (env.isTopLevel()) {
                val variable = env.resolve(name) as? ParserVariable
                    ?: throw DRVariableNotDefinedException(name = name)

                variable.set(v)
            } else {
                env.define(name, ParserVariable(name, type, v, isMutable))
            }

            v
        }

        // For loop computing
        is For -> {
            val iterable = iterable.eval(env)

            val items = when (iterable) {
                is ParserArray -> iterable.items
                is ParserInclusiveRange -> rangeToList(iterable).map { it as ParserValue }
                is ParserExclusiveRange -> rangeToList(iterable, exclusive = true).map { it as ParserValue }
                else -> throw DRCannotIterateException(type = iterable.type())
            }

            for ((index, item) in items.withIndex()) {
                val loopEnv = DrEnv(env)

                if (variables.isEmpty()) {
                    loopEnv.forceDefine("_", item)
                } else if (variables.size == 1) {
                    val name = variables[0].name

                    loopEnv.forceDefine(name, ParserVariable(name, AnyType, item, isMutable = true))
                } else if (iterable is ParserArray && variables.size == 2) {
                    val valueVariable = variables[0].name
                    val indexVariable = variables[1].name

                    loopEnv.run {
                        forceDefine(indexVariable, ParserVariable(
                            indexVariable,
                            ObjectType("Int"),
                            ParserInt(index),
                            isMutable = false))

                        forceDefine(valueVariable, ParserVariable(
                            valueVariable,
                            AnyType,
                            item,
                            isMutable = true))
                    }
                } else if (item is ParserArray && item.items.size == variables.size) {
                    variables.zip(item.items).forEach { (variable, value) ->
                        loopEnv.assign(variable.name, ParserVariable(variable.name, AnyType, value, isMutable = true))
                    }
                } else {
                    throw DRCannotDestructureException(
                        type = item.type(),
                        variablesCount = variables.size)
                }

                body.eval(loopEnv)
            }

            ParserVoid
        }

        else -> throw DRInvalidStatementException()
    }
}