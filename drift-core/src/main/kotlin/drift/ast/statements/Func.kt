/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements

import drift.ast.ParserCallable
import drift.ast.ParserReturnable
import drift.ast.bindings.FunctionParameter
import drift.ast.metadata.Annotation
import drift.runtime.AnyType
import drift.runtime.ParserType


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
 * @see drift.runtime.values.callables.ParserFunction
 * @see drift.runtime.values.callables.ParserMethod
 * @see drift.runtime.values.callables.ParserLambda
 */
data class Func(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    override val parameters: List<FunctionParameter> = mutableListOf(),
    override val body: Block = Block.empty(),
    override val returnType: ParserType = AnyType)
    : ParserStatement(),
    ParserReturnable