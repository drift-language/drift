/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements

import drift.ast.expressions.ParserExpression
import drift.runtime.ParserType


/******************************************************************************
 * DRIFT VARIABLE DECLARATION STATEMENT AST NODE
 *
 * Data class representing a variable declaration in an AST.
 ******************************************************************************/



/**
 * This class represents a variable declaration
 *
 * @property name Variable name
 * @property type Variable type
 * @property value Variable value
 * @property isMutable If the variable is mutable, can be reassigned
 */
data class Let(
    val name: String,
    val type: ParserType,
    val value: ParserExpression,
    val isMutable: Boolean) : ParserStatement()