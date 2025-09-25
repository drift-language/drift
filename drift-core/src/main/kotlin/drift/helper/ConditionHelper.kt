/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.helper

import drift.ast.expressions.Expression
import drift.runtime.evaluators.eval
import drift.exceptions.DriftRuntimeException
import drift.runtime.DrEnv
import drift.runtime.values.primaries.DrBool


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
 * @throws DriftRuntimeException If the provided condition
 * expression is not boolean
 */
fun evalCondition(condition: Expression, env: DrEnv) : Boolean {
    val conditionValue = condition.eval(env)

    if (conditionValue !is DrBool) {
        throw DriftRuntimeException("Condition must be boolean")
    }

    return conditionValue.value
}