package drift.parser

import drift.ast.*
import drift.ast.Function
import drift.ast.Set
import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.parser.statements.parseStatement
import drift.runtime.*

class Parser(private val tokens: List<Token>) {
    private var i = 0
    private var depth = 0

    internal val operatorPrecedence: Map<String, Int> = mapOf(
        "="     to 1,

        "?"     to 2,

        "=="    to 3,
        "!="    to 3,
        ">"     to 3,
        "<"     to 3,
        ">="    to 3,
        "<="    to 3,

        "+"     to 4,
        "-"     to 4,

        "*"     to 5,
        "/"     to 5,

        ".."    to 6,
    )

    internal fun current() : Token? = tokens.getOrNull(i)
    internal fun advance(
        ignoreNewLines: Boolean = true,
        ignoreWhitespaces: Boolean = true) {

        i++

        var c = current()

        while (ignoreNewLines && c == Token.NewLine
            || ignoreWhitespaces && c == Token.Whitespace) {

            i++
            c = current()
        }

        if (c is Token.Symbol) {
            if (c.value in listOf("(", "[", "{")) depth++
            else if (c.value in listOf(")", "]", "}")) depth--
        } else if (c is Token.NewLine && depth > 0) {
            advance()
        }
    }
    internal fun isAtEnd() : Boolean = current() == Token.EOL

    fun parse(): List<DrStmt> {
        val statements = mutableListOf<DrStmt>()

        while (!isAtEnd()) {
            val token = current()

            if (token is Token.NewLine) {
                advance()
                continue
            }

            val statement = parseStatement()
            statements.add(statement)

            val next = current()

            if (next is Token.NewLine || next is Token.Symbol && next.value == "}") {
                advance()
            } else if (!isAtEnd()) {
                throw DriftParserException("Expected newline after top-level statement but found $next")
            }
        }

        return statements
    }


    internal fun expectSymbol(expected: String) {
        val token = current()

        if (token !is Token.Symbol || token.value != expected) {
            throw DriftParserException("Expected '$expected' but found $token")
        }

        advance(false)
    }

    internal fun checkSymbol(value: String) : Boolean {
        val token = current()

        return token is Token.Symbol && token.value == value
    }

    internal fun matchSymbol(value: String) : Boolean {
        val token = current()

        if (token is Token.Symbol && token.value == value) {
            advance(false)

            return true
        }

        return false
    }

    internal fun peekSymbol(
        value: String,
        ignoreNewLines: Boolean = false,
        ignoreWhitespaces: Boolean = true) : Boolean {

        var j = i + 1

        while (ignoreNewLines && tokens.getOrNull(j) == Token.NewLine
            || ignoreWhitespaces && tokens.getOrNull(j) == Token.Whitespace) {

            j++
        }

        val next = tokens.getOrNull(j)

        return next is Token.Symbol && next.value == value
    }

    internal inline fun <reified T : Token> expect(message: String) : T {
        val token = current()

        return token as? T ?: throw DriftParserException("Expected '$message' but found $token")
    }

    internal fun isLambda() : Boolean {
        if (!checkSymbol("("))
            return false

        var j = i + 1

        fun localAdvance() {
            do {
                j++
            } while (tokens.getOrNull(j) == Token.NewLine
                || tokens.getOrNull(j) == Token.Whitespace)
        }

        var depth = 1

        while (j < tokens.size) {
            val token: Token? = tokens.getOrNull(j)

            if (token is Token.Symbol) {
                when (token.value) {
                    "(" -> depth++
                    ")" -> {
                        depth--

                        if (depth == 0) {
                            localAdvance()
                            break
                        }
                    }
                }
            }

            localAdvance()
        }

        if (tokens.getOrNull(j)?.let { it is Token.Symbol && it.value == ":" } == true) {
            localAdvance()

            while (j < tokens.size) {
                val t = tokens[j]

                val isTypePart = when (t) {
                    is Token.Identifier -> true
                    is Token.Symbol -> t.value in listOf("?", "|")
                    else -> false
                }

                if (!isTypePart) break

                localAdvance()
            }
        }

        val hasArrow = (tokens[j] as? Token.Symbol)?.value == "->"

        return tokens.getOrNull(j) is Token.Symbol && hasArrow
    }

    internal fun skip(token: Token) {
        while (current() == token)
            advance()
    }
}