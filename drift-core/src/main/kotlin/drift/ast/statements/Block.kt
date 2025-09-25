/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements


/******************************************************************************
 * DRIFT BLOCK AST NODE
 *
 * Data class representing a block (scope) in an AST.
 ******************************************************************************/



/**
 * A block is a statement container allowing
 * to compute statements into an isolated environment
 * instance, inherited by parents.
 *
 * @property statements Block statements
 */
data class Block(val statements: List<DrStmt>) : DrStmt