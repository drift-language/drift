package drift.parser.classes

import drift.ast.Class
import drift.ast.Function
import drift.ast.FunctionParameter
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.functions.parseFunction
import drift.parser.types.parseType

internal fun Parser.parseClass() : Class {
    val nameToken = expect<Token.Identifier>("Expected class name")
    val name = nameToken.value
    val fields = mutableListOf<FunctionParameter>()
    val methods = mutableListOf<Function>()

    advance(false)

    if (matchSymbol("(")) {
        if (!checkSymbol(")")) {
            do {
                val paramToken = expect<Token.Identifier>("Expected field name")

                advance()

                expectSymbol(":")
                val fieldType = parseType()

                fields.add(FunctionParameter(paramToken.value, true, fieldType))
            } while (matchSymbol(","))
        }

        expectSymbol(")")
    }

    if (matchSymbol("{")) {
        while (!matchSymbol("}")) {
            val c = current()

            if (c !is Token.Identifier || !c.isKeyword(Token.Keyword.FUNCTION)) {
                throw DriftParserException("Only methods are allowed inside class body")
            }

            advance()

            methods.add(parseFunction())

            if (current() is Token.NewLine) advance()
        }
    }

    return Class(name, fields, methods)
}