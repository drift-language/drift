/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.helper

import drift.ast.expressions.ParserExpression
import drift.runtime.DrEnv
import drift.runtime.evaluators.eval
import drift.runtime.exceptions.DRNotSupportedTypeInBooleanExpressionException
import drift.runtime.values.primaries.ParserBool


/******************************************************************************
 * CONDITION HELPER FUNCTIONS
 *
 * All functions which help to manipulate AST conditional objects
 * are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse a conditional expression
 * using provided environment instance
 *
 * @param condition Condition to evaluate
 * @param env Environment instance to use
 * @return If the condition is respected or not
 * @throws DRNotSupportedTypeInBooleanExpressionException If the provided condition
 * expression is not boolean
 */
fun evalCondition(condition: ParserExpression, env: DrEnv) : Boolean {
    val conditionValue = condition.eval(env)

    if (conditionValue !is ParserBool) {
        throw DRNotSupportedTypeInBooleanExpressionException(
            type = conditionValue.type())
    }

    return conditionValue.value
}