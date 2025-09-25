/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.containers

import drift.ast.expressions.Expression
import drift.ast.expressions.ListLiteral
import drift.parser.Parser
import drift.parser.Token
import drift.parser.expressions.parseExpression


/******************************************************************************
 * DRIFT CONTAINERS PARSER
 *
 * All methods permitting to parse containers are define in this file.
 * A container permits to contain many values.
 ******************************************************************************/



/**
 * AST representation of a list
 *
 * @return List AST node
 */
internal fun Parser.parseList() : ListLiteral {
    expectSymbol("[")

    val values = mutableListOf<Expression>()

    if (!checkSymbol("]")) {
        do {
            skip(Token.NewLine)

            values.add(parseExpression())

            skip(Token.NewLine)
        } while (matchSymbol(","))
    }

    expectSymbol("]")

    return ListLiteral(values)
}