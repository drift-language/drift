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
 * DRIFT CALL AST NODE
 *
 * Data class representing an entity call in an AST.
 ******************************************************************************/



/**
 * This class represents a callable call
 *
 * @property callee Callable name
 * @property args Callable arguments list
 */
data class Call(val callee: Expression, val args: List<Argument>) : Expression