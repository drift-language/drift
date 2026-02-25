/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.annotations

import drift.ast.expressions.Argument
import drift.ast.metadata.Annotation
import drift.lexer.Token
import drift.parser.Parser
import drift.parser.expressions.parseArguments

/******************************************************************************
 * DRIFT ANNOTATIONS PARSER METHODS
 *
 * All methods permitting parse annotations are defined in this file.
 ******************************************************************************/



internal fun Parser.parseAnnotation() : Annotation {
    val nameToken = expect<Token.Annotation>("annotation name")
    val name = nameToken.name
    var args = listOf<Argument>()

    advance(false)

    if (matchSymbol("("))
        args = parseArguments()

    return Annotation(name, args)
}