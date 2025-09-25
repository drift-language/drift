/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements

import drift.ast.expressions.Expression
import drift.runtime.*


/******************************************************************************
 * DRIFT FUNCTION STRUCTURES
 *
 * All Drift function structures are defined in this file.
 ******************************************************************************/



/**
 * This class represents a callable structure
 *
 * @property name Callable name
 * @property parameters Callable arguments structures
 * @property body Callable body AST
 * @property returnType Callable return type
 * @see drift.runtime.values.callables.DrFunction
 * @see drift.runtime.values.callables.DrMethod
 * @see drift.runtime.values.callables.DrLambda
 */
data class Function(
    val name: String,
    val parameters: List<FunctionParameter>,
    val body: List<DrStmt>,
    val returnType: DrType = AnyType) : DrStmt



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
    val name: String,
    val isPositional: Boolean = false,
    val type: DrType = AnyType,
    val defaultValue: Expression? = null)