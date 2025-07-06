package drift.parser.functions

import drift.ast.DrExpr
import drift.ast.Function
import drift.ast.FunctionParameter
import drift.ast.Lambda
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.statements.parseBlock
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.DrType

internal fun Parser.parseFunction() : Function {
    val nameToken = expect<Token.Identifier>("Expected function name")
    val name = nameToken.value
    val parameters = mutableListOf<FunctionParameter>()

    advance()

    if (matchSymbol("(")) {
        if (!checkSymbol(")")) {
            do {
                val isPositional: Boolean = matchSymbol("*")
                val paramToken = expect<Token.Identifier>("Expected parameter name")

                if (parameters.firstOrNull { it.name == paramToken.value } != null)
                    throw DriftParserException("Parameter ${paramToken.value} is already defined")

                advance()

                var paramType: DrType = AnyType

                if (matchSymbol(":")) {
                    paramType = parseType()
                }

                parameters.add(FunctionParameter(paramToken.value, isPositional, paramType))
            } while (matchSymbol(","))
        }

        expectSymbol(")")
    }

    val returnType: DrType =
        if (matchSymbol(":")) parseType()
        else AnyType

    val body = parseBlock().statements

    return Function(name, parameters, body, returnType)
}

internal fun Parser.parseLambda() : DrExpr {
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

    var returnType: DrType = AnyType

    if (matchSymbol(":")) {
        returnType = parseType()
    }

    expectSymbol("->")

    val body = parseBlock().statements

    return Lambda(null, parameters, body, returnType)
}