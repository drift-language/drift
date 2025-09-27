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
 * DRIFT OBJECT SETTER AST NODE
 *
 * Data class representing an object setter in an AST.
 ******************************************************************************/



/**
 * A set structure represents an object field assignment
 *
 * @property receiver Object where the field is defined
 * @property name Field name to assign
 * @property value Value to assign
 */
data class Set(
    val receiver: Expression,
    val name: String,
    val value: Expression) : Expression