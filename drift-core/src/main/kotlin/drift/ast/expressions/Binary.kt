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
 * DRIFT BINARY AST NODE
 *
 * Data class representing a binary expression in an AST.
 ******************************************************************************/



/**
 * A binary structure represents an operation
 * with two operands and an operator
 *
 * @property left Left operand
 * @property operator Operator
 * @property right Right operand
 */
data class Binary(val left: Expression, val operator: String, val right: Expression) : Expression