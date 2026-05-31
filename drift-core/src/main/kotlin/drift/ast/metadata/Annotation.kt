/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.metadata

import drift.ast.ParserNode
import drift.ast.expressions.Argument

data class Annotation(
    val name: String,
    val args: List<Argument>) : ParserNode()