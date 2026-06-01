/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast

import drift.ast.bindings.FunctionParameter
import drift.ast.statements.Block


interface ParserCallable {

    val parameters: List<FunctionParameter>
    val body: Block
}