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
 * DRIFT ASSIGN AST NODE
 *
 * Data class representing an assignment statement in an AST.
 ******************************************************************************/



/**
 * An assign structure contains the variable name
 * and value
 *
 * @property name Variable name
 * @property value Value to assign
 */
data class Assign(
    val name: String,
    val value: Expression) : Expression