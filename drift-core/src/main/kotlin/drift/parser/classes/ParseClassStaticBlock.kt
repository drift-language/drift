/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.classes

import drift.ast.statements.DrStmt
import drift.ast.statements.Function
import drift.ast.statements.FunctionParameter
import drift.ast.statements.Let
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.callables.parseFunction
import drift.parser.statements.parseLet


/******************************************************************************
 * DRIFT CLASSES STATIC BLOCK PARSER
 *
 * All methods permitting parse classes static block are defined in this file.
 ******************************************************************************/



internal fun Parser.parseClassStaticBlock(
    staticFields: MutableList<Let>,
    staticMethods: MutableList<Function>) {

    val c = current()

    if (c is Token.Identifier) when {
        c.isKeyword(Token.Keyword.IMMUTLET) ||
        c.isKeyword(Token.Keyword.MUTLET) -> {
            advance(false)

            staticFields += parseLet(c.isKeyword(Token.Keyword.MUTLET))
        }

        c.isKeyword(Token.Keyword.FUNCTION) -> {
            advance(false)

            staticMethods += parseFunction()
        }

        else -> throw DriftParserException("Unexpected '${c.value}' in static block")
    }
}