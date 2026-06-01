/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.expressions

import drift.ast.ParserReturnable
import drift.ast.bindings.FunctionParameter
import drift.ast.statements.Block
import drift.oldruntime.AnyType
import drift.oldruntime.ParserType


/******************************************************************************
 * DRIFT LAMBDA AST NODE
 *
 * Data class representing a lambda in an AST.
 ******************************************************************************/



/**
 * A lambda is a callable expression that can be stored
 * in a variable or returned by another function,
 * for example
 *
 * @property parameters Lambda arguments structures
 * @property body Lambda body AST
 * @property returnType Lambda return type
 */
data class Lambda(
    override val parameters: List<FunctionParameter> = emptyList(),
    override val body: Block = Block.empty(),
    override val returnType: ParserType = AnyType)
    : ParserExpression(),
    ParserReturnable