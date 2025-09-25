/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements

import drift.runtime.AnyType
import drift.runtime.DrType


/******************************************************************************
 * DRIFT FUNCTION DECLARATION STATEMENT AST NODE
 *
 * Data class representing a function declaration in an AST.
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
    val returnType: DrType = AnyType
) : DrStmt