/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.expressions


/******************************************************************************
 * DRIFT UNARY AST NODE
 *
 * Data class representing an unary expression in an AST.
 ******************************************************************************/



/**
 * A unary expression represents an expression
 * with an operator which is applied to once
 * operand
 *
 * @property operator Unary operator
 * @property expr Expression
 */
data class Unary(val operator: String, val expr: Expression) : Expression