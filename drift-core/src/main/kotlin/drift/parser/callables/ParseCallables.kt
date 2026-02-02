/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.callables

import drift.ast.expressions.ParserExpression
import drift.ast.expressions.Lambda
import drift.ast.statements.Function
import drift.ast.bindings.FunctionParameter
import drift.parser.Parser
import drift.lexer.Token
import drift.parser.exceptions.*
import drift.parser.expressions.parseExpression
import drift.parser.statements.parseBlock
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.ParserType


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
    val nameToken = expect<Token.Identifier>("function name")
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

    val returnType: ParserType =
        if (matchSymbol(":")) parseType()
        else AnyType

    expectSymbol("{")

    val body = parseBlock().statements

    return Function(name, parameters, body, returnType)
}



/**
 * Attempt to parse a lambda expression.
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
 * - If a parameter's part is unterminated, without ')' symbol
 * - If the arrow is absent '->'
 * @throws DPParameterAlreadyDefinedException One case may throw:
 * - If the provided parameter's name is previously defined
 */
internal fun Parser.parseLambda() : Lambda {
    expectSymbol("(")

    val parameters = mutableListOf<FunctionParameter>()

    if (!checkSymbol(")")) {
        do {
            val paramToken = expect<Token.Identifier>("parameter name")

            if (parameters.firstOrNull { it.name == paramToken.value } != null)
                throw DPParameterAlreadyDefinedException(
                    parameterName = paramToken.value)

            advance()

            val paramType =
                if (matchSymbol(":")) parseType()
                else AnyType

            parameters.add(FunctionParameter(
                paramToken.value,
                isPositional = true,
                paramType))
        } while (matchSymbol(","))
    }

    expectSymbol(")")

    val returnType: ParserType =
        if (matchSymbol(":")) parseType()
        else AnyType

    expectSymbol("->"); expectSymbol("{")

    val body = parseBlock().statements

    return Lambda(parameters, body, returnType)
}



/**
 * Attempt to parse a function parameter expression.
 *
 * @throws DPParameterAlreadyDefinedException One case may throw:
 * - If the provided parameter's name is previously defined
 */
internal fun Parser.parseFunctionParameter(parameters: MutableList<FunctionParameter>) : FunctionParameter {
    val isPositional: Boolean = matchSymbol("*")
    val paramToken = expect<Token.Identifier>("parameter name")
    var value: ParserExpression? = null

    if (parameters.firstOrNull { it.name == paramToken.value } != null)
        throw DPParameterAlreadyDefinedException(
            parameterName = paramToken.value)

    advance()

    var paramType: ParserType = AnyType

    if (matchSymbol(":"))
        paramType = parseType()

    if (matchSymbol("="))
        value = parseExpression()

    return FunctionParameter(paramToken.value, isPositional, paramType, value)
}