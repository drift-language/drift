/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements.hooks

import drift.ast.ParserCallable
import drift.ast.ParserReturnable
import drift.ast.bindings.FunctionParameter
import drift.ast.metadata.Annotation
import drift.ast.statements.Block
import drift.ast.statements.ParserStatement
import drift.runtime.ParserType

data class ReturnableHook(
    override val name: String,
    override val annotations: MutableList<Annotation> = mutableListOf(),
    override val parameters: List<FunctionParameter> = listOf(),
    override val body: Block = Block.empty(),
    override val returnType: ParserType)
    : ParserStatement(), ParserHook, ParserReturnable
