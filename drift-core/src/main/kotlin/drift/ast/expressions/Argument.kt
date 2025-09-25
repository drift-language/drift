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
 * DRIFT CALL ARGUMENT AST NODE
 *
 * Data class representing a call argument in an AST.
 ******************************************************************************/



/**
 * An argument structure represent a call argument
 *
 * @property name Argument name
 * @property expr Argument value expression
 */
data class Argument(
    val name: String?,
    val expr: Expression)