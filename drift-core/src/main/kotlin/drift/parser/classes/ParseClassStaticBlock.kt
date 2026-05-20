/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.classes

import drift.ast.statements.Func
import drift.ast.statements.Let
import drift.parser.Parser
import drift.lexer.Token
import drift.parser.callables.parseFunction
import drift.parser.exceptions.DPUnexpectedExpressionException
import drift.parser.exceptions.DPUnexpectedIdentifierException
import drift.parser.statements.parseLet


/******************************************************************************
 * DRIFT CLASSES STATIC BLOCK PARSER
 *
 * All methods permitting parse classes static block are defined in this file.
 ******************************************************************************/



internal fun Parser.parseClassStaticBlock(
    staticFields: MutableList<Let>,
    staticMethods: MutableList<Func>) {

    val c = current()

    if (c is Token.Identifier) when {
        c.isKeyword(Token.Keyword.IMMUTLET) ||
        c.isKeyword(Token.Keyword.MUTLET) -> {
            advance(false)

            staticFields += parseLet(
                isMutable = c.isKeyword(Token.Keyword.MUTLET),
                acceptUnassigned = false)
        }

        c.isKeyword(Token.Keyword.FUNCTION) -> {
            advance(false)

            staticMethods += parseFunction()
        }

        else -> throw DPUnexpectedIdentifierException(
            unexpected = c,
            context = "in static block")
    } else {
        throw DPUnexpectedExpressionException(
            unexpected = c,
            context = "in static block")
    }
}