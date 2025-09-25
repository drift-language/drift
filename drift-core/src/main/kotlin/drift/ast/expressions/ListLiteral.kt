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
 * DRIFT LIST LITERAL AST NODE
 *
 * Data class representing a list and its values in an AST.
 ******************************************************************************/



/**
 * A list structure
 *
 * @property values List values
 */
data class ListLiteral(val values: MutableList<Expression>) : Expression