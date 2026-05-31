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
import drift.ast.metadata.Annotation

interface ParserHook : ParserCallable {

    val name: String
    val annotations: MutableList<Annotation>
}