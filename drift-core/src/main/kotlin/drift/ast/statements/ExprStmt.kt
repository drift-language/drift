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
 * DRIFT EXPRESSION STATEMENT AST NODE
 *
 * Data class representing an expression as statement in an AST.
 ******************************************************************************/



/**
 * This class represents a statement exclusively
 * composed by an expression, like a function call.
 *
 * ```
 * call()  // This is a expression statement
 * ```
 *
 * @property expr Statement expression
 */
data class ExprStmt(val expr: Expression) : DrStmt