package drift.parser

import drift.exceptions.DriftLexerException
import drift.utils.concat

private val multiCharsSymbols = setOf(
    "==", "!=",
    "<=", ">=",
    "&&", "||",
)

private val singleCharSymbols = setOf(
    '(', ')',
    '{', '}',
    '+', '-',
    '*', '/',
    '=', ':',
    '?', ',',
    '<', '>',
    '|', '&',
    '?', '!'
)

sealed class Token {
    data class Identifier(val value: String) : Token() {
        fun isKeyword(expected: Token.Keyword): Boolean =
            this.value == expected.value
    }
    data class StringLiteral(val value: String) : Token()
    data class IntLiteral(val value: Int) : Token()
    data class BoolLiteral(val value: Boolean) : Token()
    data object NullLiteral : Token()
    data class Symbol(val value: String) : Token()
    enum class Keyword(val value: String) {
        IF("if"),
        ELSE("else"),
        FUNCTION("fun"),
        RETURN("return"),
        FOR("for"),
        CLASS("class"),
    }
    data object EOL : Token()
    data object NewLine : Token()
}

fun lex(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0

    while (i < input.length) {
        val c = input[i]

        if (c == '\r' && input[i + 1] == '\n') {
            tokens.add(Token.NewLine)
            i += 2
            continue
        }

        if (c == '\n' || c == '\r') {
            tokens.add(Token.NewLine)
            i++
            continue
        }

        if (c.isWhitespace()) {
            i++
            continue
        }

        if (c == '"') {
            val (token, next) = lexString(input, i)
            tokens.add(token)

            i = next
            continue
        }

        if (c.isDigit()) {
            val (token, next) = lexDigit(input, i)
            tokens.add(token)

            i = next
            continue
        }

        if (c.isLetter() || c == '_') {
            val (token, next) = lexWord(input, i)
            tokens.add(token)

            i = next
            continue
        }

        val twoChar = input.substring(i).take(2)

        if (twoChar in multiCharsSymbols) {
            val (token, next) = lexSymbol(input, i, 2)
            tokens.add(token)

            i = next
            continue
        } else if (c in singleCharSymbols) {
            val (token, next) = lexSymbol(input, i)
            tokens.add(token)

            i = next
            continue
        }
    }

    tokens.add(Token.EOL)

    return tokens
}

fun lexString(input: String, startIndex: Int): Pair<Token.StringLiteral, Int> {
    var i = startIndex + 1
    val start = i

    while (i < input.length && input[i] != '"') {
        i++
    }

    if (i >= input.length) {
        throw DriftLexerException("Unterminated string literal")
    }

    val content = input.substring(start, i)

    return Token.StringLiteral(content) to (i + 1)
}

fun lexDigit(input: String, startIndex: Int): Pair<Token.IntLiteral, Int> {
    var i = startIndex
    var final: Int = 0

    while (i < input.length && input[i].isDigit()) {
        final = final concat input[i].digitToInt()
        i++
    }

    return Token.IntLiteral(final) to i
}

fun lexWord(input: String, startIndex: Int): Pair<Token, Int> {
    var i = startIndex

    while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) {
        i++
    }

    val word = input.substring(startIndex, i)

    return when (word) {
        "true" -> Token.BoolLiteral(true) to i
        "false" -> Token.BoolLiteral(false) to i
        "null" -> Token.NullLiteral to i
        else -> Token.Identifier(word) to i
    }
}

fun lexSymbol(input: String, startIndex: Int, length: Int = 1): Pair<Token.Symbol, Int> {
    val symbol = input.substring(startIndex, startIndex + length)

    return Token.Symbol(symbol) to (startIndex + length)
}