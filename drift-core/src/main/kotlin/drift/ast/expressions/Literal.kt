/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.expressions

import drift.runtime.DrValue


/******************************************************************************
 * DRIFT LITERAL AST NODE
 *
 * Data class representing a literal in an AST.
 ******************************************************************************/



/**
 * A literal expression directly contains a value
 *
 * @property value Literal value
 */
data class Literal(val value: DrValue) : Expression