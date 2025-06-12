package drift.parser

import drift.ast.*
import drift.runtime.*

class Parser(private val tokens: List<Token>) {
    private var i = 0

    private val operatorPrecedence: Map<String, Int> = mapOf(
        "==" to 1,
        "!=" to 1,
        ">" to 1,
        "<" to 1,
        ">=" to 1,
        "<=" to 1,

        "+" to 2,
        "-" to 2,

        "*" to 3,
        "/" to 3,
    )

    private fun current() : Token? = tokens.getOrNull(i)
    private fun advance() { i++ }

    fun parse(): DrStmt = parseStatement()

    private fun parseStatement() : DrStmt {
        val token = current()

        return when (token) {
            is Token.Symbol -> when (token.value) {
                "{" -> parseBlock()
                else -> ExprStmt(parseExpression())
            }
            is Token.Identifier -> when {
                token.isKeyword(Token.Keyword.IF) -> parseClassicIf()
                else -> ExprStmt(parseExpression())
            }
            else -> ExprStmt(parseExpression())
        }
    }

    private fun parsePrimary() : DrExpr {
        return when (val token = current()) {
            is Token.StringLiteral -> { advance(); Literal(DrString(token.value)) }
            is Token.IntLiteral -> { advance(); Literal(DrInt(token.value)) }
            is Token.BoolLiteral -> { advance(); Literal(DrBool(token.value)) }
            is Token.Identifier -> parseCallOrVariable()
            is Token.Symbol -> when (token.value) {
                "(" -> {
                    advance()

                    val expression = parseExpression()
                    expectSymbol(")")
                    expression
                }
                else -> error("Unexpected token ${token.value}")
            }
            else -> error("Unexpected token $token")
        }
    }

    private fun parseExpression(minPrecedence: Int = 0) : DrExpr {
        var left = parsePrimary()

        if (current() is Token.Symbol && (current() as Token.Symbol).value == "?") {
            return parseDriftIf(left)
        }

        while (true) {
            val opToken = current()

            if (opToken !is Token.Symbol || opToken.value !in operatorPrecedence)
                break

            val precedence = operatorPrecedence[opToken.value] ?: 0

            if (precedence < minPrecedence)
                break

            val op = opToken.value

            advance()

            val right = parseExpression(precedence + 1)

            left = Binary(left, op, right)
        }

        return left
    }

    private fun parseCallOrVariable() : DrExpr {
        val id = current() as Token.Identifier

        advance()

        return if (current() is Token.Symbol && (current() as Token.Symbol).value == "(") {
            advance()

            val args = mutableListOf<DrExpr>()

            while (current() !is Token.Symbol || (current() as Token.Symbol).value != ")") {
                args.add(parseExpression())

                if (current() is Token.Symbol && (current() as Token.Symbol).value == ",") {
                    advance()
                } else {
                    break
                }
            }

            if (current() !is Token.Symbol || (current() as Token.Symbol).value != ")") {
                error("Expected ')' after arguments")
            }

            advance()

            Call(Variable(id.value), args)
        } else {
            Variable(id.value)
        }
    }

    private fun parseBlock() : DrStmt {
        val open = current()

        if (open !is Token.Symbol || open.value != "{") {
            error("Expected '{' but found $open")
        }

        advance()

        val statements = mutableListOf<DrStmt>()

        while (true) {
            val token = current() ?: error("Unterminated block, expected '}'")

            if (token is Token.Symbol && token.value == "}") {
                advance()
                break
            }

            val statement = parseStatement()
            statements.add(statement)
        }

        return Block(statements)
    }

    private fun parseStatementOrBlock() : DrStmt {
        return if (current() is Token.Symbol && (current() as Token.Symbol).value == "{") {
            parseBlock()
        } else {
            ExprStmt(parseExpression())
        }
    }

    private fun parseClassicIf() : DrStmt {
        val token = current()

        if (token !is Token.Identifier || !token.isKeyword(Token.Keyword.IF)) {
            error("Expected 'if' but found $token")
        }

        advance()

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

    private fun parseDriftIf(condition: DrExpr) : DrExpr {
        advance()

        val thenBlock = parseStatementOrBlock()
        var elseBlock: DrStmt? = null

        if (current() is Token.Symbol && (current() as Token.Symbol).value == ":") {
            advance()
            elseBlock = parseStatementOrBlock()
        }

        return Conditional(condition, thenBlock, elseBlock)
    }


    private fun expectSymbol(expected: String) {
        val token = current()

        if (token !is Token.Symbol || token.value != expected) {
            error("Expected '$expected' but found $token")
        }

        advance()
    }
}