/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.callables

import drift.ast.*
import drift.ast.Function
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.expressions.parseExpression
import drift.parser.statements.parseBlock
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.values.specials.DrNotAssigned


/******************************************************************************
 * DRIFT CALLABLES PARSER METHODS
 *
 * All methods permitting to parse callables are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse a function definition expression
 *
 * ```
 * fun test(arg) : Type {
 *      statement
 * }
 * ```
 *
 * @return Constructed function definition statement
 * AST object
 * @throws DriftParserException Two cases may throw:
 * - If none name is provided for the function
 * - If none name is provided for a parameter `fun test(*)`
 * for example
 */
internal fun Parser.parseFunction() : Function {
    val nameToken = expect<Token.Identifier>("Expected function name")
    val name = nameToken.value
    val parameters = mutableListOf<FunctionParameter>()

    advance()

    if (matchSymbol("(")) {
        if (!checkSymbol(")")) {
            do {
                parameters.add(parseFunctionParameter(parameters))
            } while (matchSymbol(","))
        }

        expectSymbol(")")
    }

    val returnType: DrType =
        if (matchSymbol(":")) parseType()
        else AnyType

    expectSymbol("{")

    val body = parseBlock().statements

    return Function(name, parameters, body, returnType)
}



/**
 * Attempt to parse a lambda expression
 *
 * ```
 * let lambda = (arg) : Type -> {
 *      statement
 * }
 * ```
 *
 * @return Constructed lambda expression AST object
 * @throws DriftParserException Many cases may throw:
 * - If the expression does not begin by a '(' symbol
 * - If none name is provided for a parameter `(*) -> {}`
 * for example
 * - If a parameter is duplicated within lambda definition
 * - If the parameters part is unterminated, without ')' symbol
 * - If the arrow is absent '->'
 */
internal fun Parser.parseLambda() : Lambda {
    expectSymbol("(")

    val parameters = mutableListOf<FunctionParameter>()

    if (!checkSymbol(")")) {
        do {
            val paramToken = expect<Token.Identifier>("Expected parameter name")

            if (parameters.firstOrNull { it.name == paramToken.value } != null)
                throw DriftParserException("Parameter ${paramToken.value} is already defined")

            advance()

            val paramType = if (matchSymbol(":")) {
                parseType()
            } else {
                AnyType
            }

            parameters.add(FunctionParameter(paramToken.value, isPositional = true, paramType))
        } while (matchSymbol(","))
    }

    expectSymbol(")")

    val returnType: DrType =
        if (matchSymbol(":")) parseType()
        else AnyType

    expectSymbol("->"); expectSymbol("{")

    val body = parseBlock().statements

    return Lambda(null, parameters, body, returnType)
}


internal fun Parser.parseFunctionParameter(parameters: MutableList<FunctionParameter>) : FunctionParameter {
    val isPositional: Boolean = matchSymbol("*")
    val paramToken = expect<Token.Identifier>("Expected parameter name")
    var value: DrExpr? = null

    if (parameters.firstOrNull { it.name == paramToken.value } != null)
        throw DriftParserException("Parameter ${paramToken.value} is already defined")

    advance()

    var paramType: DrType = AnyType

    if (matchSymbol(":")) {
        paramType = parseType()
    }

    if (matchSymbol("=")) {
        value = parseExpression()
    }

    return FunctionParameter(paramToken.value, isPositional, paramType, value)
}