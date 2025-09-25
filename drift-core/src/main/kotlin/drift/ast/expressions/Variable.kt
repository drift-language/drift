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
 * DRIFT VARIABLE AST NODE
 *
 * Data class representing a literal in an AST.
 ******************************************************************************/



/**
 * A variable is represented by a name
 *
 * @property name Variable name
 */
data class Variable(val name: String) : Expression