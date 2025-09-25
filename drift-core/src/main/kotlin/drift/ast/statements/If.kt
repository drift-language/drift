/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements

import drift.ast.expressions.Expression


/******************************************************************************
 * DRIFT IF CONDITIONAL STATEMENT AST NODE
 *
 * Data class representing an IF statement in an AST.
 ******************************************************************************/



/**
 * This class represents a classic conditional statement.
 *
 * The Drift-style structure is an expression,
 * and is computed using [ExprStmt].
 *
 * @property condition Condition to compute
 * @property thenBranch Branch to execute if the
 * condition is successful
 * @property elseBranch Branch to execute if the
 * condition is unsuccessful
 */
data class If(
    val condition: Expression,
    val thenBranch: DrStmt,
    val elseBranch: DrStmt?) : DrStmt