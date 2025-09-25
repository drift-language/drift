/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.expressions

import drift.ast.statements.DrStmt


/******************************************************************************
 * DRIFT CONDITIONAL AST NODE
 *
 * Data class representing a conditional structure in an AST.
 ******************************************************************************/



/**
 * A ternary conditional structure contains a condition,
 * a then and else branches.
 *
 * It is the recommended syntax for conditional evaluation,
 * instead of [drift.ast.statements.If].
 *
 * @property condition Condition
 * @property thenBranch Branch to execute if the
 * condition is successful
 * @property elseBranch Branch to execute if the
 * condition is unsuccessful
 */
data class Conditional(
    val condition: Expression,
    val thenBranch: DrStmt,
    val elseBranch: DrStmt?) : Expression