/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.modifiers

import drift.ast.statements.Func
import drift.ast.statements.modifiers.NativeModifier
import drift.parser.Parser
import drift.parser.callables.parseFunction


internal fun Parser.parseNativeModifier() : Func {
    storedModifiers.add(NativeModifier)

    advance(false)

    return parseFunction()
}