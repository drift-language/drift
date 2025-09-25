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
 * DRIFT OBJECT GETTER AST NODE
 *
 * Data classe representing an object getter in an AST.
 ******************************************************************************/



/**
 * A get structure represents an object field access
 *
 * @property receiver Object where the field is defined
 * @property name Field name to retrieve
 */
data class Get(val receiver: Expression, val name: String) : Expression