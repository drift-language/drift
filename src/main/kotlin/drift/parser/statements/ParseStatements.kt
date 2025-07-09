package drift.parser.statements

import drift.ast.*
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.classes.parseClass
import drift.parser.expressions.parseExpression
import drift.parser.functions.parseFunction
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.values.specials.DrNotAssigned

internal fun Parser.parseStatement() : DrStmt {
    return when (val token = current()) {
        is Token.Symbol -> when (token.value) {
            "{" -> parseBlock()
            else -> ExprStmt(parseExpression())
        }
        is Token.Identifier -> when {
            token.isKeyword(Token.Keyword.IF) -> {
                advance()
                parseClassicIf()
            }
            token.isKeyword(Token.Keyword.FUNCTION) -> {
                advance()
                parseFunction()
            }
            token.isKeyword(Token.Keyword.RETURN) -> {
                advance()
                parseReturn()
            }
            token.isKeyword(Token.Keyword.FOR) -> {
                advance()
                parseFor()
            }
            token.isKeyword(Token.Keyword.CLASS) -> {
                advance()
                parseClass()
            }
            token.isKeyword(Token.Keyword.IMMUTLET) -> {
                advance()
                parseLet(false)
            }
            token.isKeyword(Token.Keyword.MUTLET) -> {
                advance()
                parseLet(true)
            }
            else -> ExprStmt(parseExpression())
        }
        else -> ExprStmt(parseExpression())
    }
}

internal fun Parser.parseLet(isMutable: Boolean) : DrStmt {
    val nameToken = expect<Token.Identifier>("Expected variable name")
    val name = nameToken.value

    advance(peekSymbol(":", true) || peekSymbol("=", true))

    val type : DrType = if (matchSymbol(":")) {
        parseType()
    } else {
        AnyType
    }

    if (peekSymbol("="))
        skip(Token.NewLine)

    var expr = if (matchSymbol("=")) {
        parseExpression()
    } else {
        Literal(DrNotAssigned)
    }

    if (expr is Lambda) {
        expr = expr.copy(name)
    }

    return Let(name, type, expr, isMutable)
}

internal fun Parser.parseStatementOrBlock() : DrStmt {
    return if (current() is Token.Symbol && (current() as Token.Symbol).value == "{") {
        parseBlock()
    } else {
        ExprStmt(parseExpression())
    }
}

internal fun Parser.parseClassicIf() : If {
    val condition = parseExpression()
    val thenBlock = parseBlock()
    var elseBlock: DrStmt? = null

    if (current() is Token.Identifier
        && (current() as Token.Identifier).isKeyword(Token.Keyword.ELSE)) {

        advance()
        elseBlock = parseBlock()
    }

    return If(condition, thenBlock, elseBlock)
}

internal fun Parser.parseReturn() : DrStmt =
    Return(parseExpression())

internal fun Parser.parseBlock() : Block {
    val open = current()

    if (open !is Token.Symbol || open.value != "{") {
        throw DriftParserException("Expected '{' but found $open")
    }

    advance()

    val statements = mutableListOf<DrStmt>()

    while (true) {
        val token = current()
            ?: throw DriftParserException("Unterminated block, expected '}'")

        if (token is Token.Symbol && token.value == "}") {
            advance(false)
            break
        }

        if (token is Token.NewLine) {
            advance(false)
            continue
        }

        val statement = parseStatement()
        statements.add(statement)

        val next = current()

        when (next) {
            is Token.NewLine -> advance(false)
            is Token.Symbol -> if (next.value != "}")
                throw DriftParserException("Expected newline or '}' after statement but found $next")
            else -> throw DriftParserException("Expected newline or '}' after statement but found $next")
        }
    }

    return Block(statements)
}

internal fun Parser.parseFor() : DrStmt {
    val iterable = parseExpression()

    expectSymbol("{")

    val variables = mutableListOf<String>()

    val c = current()

    if (c is Token.Identifier && c.isKeyword(Token.Keyword.AS)) {
        advance(false)

        do {
            val name = expect<Token.Identifier>(
                "Expected variable name after '${Token.Keyword.AS}'").value

            variables.add(name)

            advance(false)
        } while (matchSymbol(","))
    }

    val statements = mutableListOf<DrStmt>()

    while (!checkSymbol("}")) {
        statements.add(parseStatement())
    }

    expectSymbol("}")

    return For(iterable, variables, Block(statements))
}