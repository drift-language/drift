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
import drift.ast.bindings.FunctionParameter
import drift.ast.metadata.Annotation
import drift.ast.statements.Block
import drift.ast.statements.ParserStatement


data class UnreturnableHook(
    override val name: String,
    override val annotations: MutableList<Annotation> = mutableListOf(),
    override val parameters: List<FunctionParameter> = listOf(),
    override val body: Block = Block.empty())
    : ParserStatement(), ParserCallable, Hook