/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.bindings

import drift.ast.expressions.ParserExpression
import drift.oldruntime.AnyType
import drift.oldruntime.ParserType


/******************************************************************************
 * DRIFT FUNCTION PARAMETER AST NODE
 *
 * Data class representing a function parameter in an AST.
 ******************************************************************************/



/**
 * This class represents a callable argument
 *
 * @property name Argument name
 * @property isPositional If the argument name
 * must always be written on call
 * @property type Argument type
 * @property defaultValue Default value assigned to parameter
 */
data class FunctionParameter(
    override val name: String,
    val isPositional: Boolean = false,
    val type: ParserType = AnyType,
    val defaultValue: ParserExpression? = null) : VariableBinding(name)