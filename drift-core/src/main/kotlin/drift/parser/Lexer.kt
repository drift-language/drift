/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser

import drift.exceptions.DriftLexerException


/******************************************************************************
 * MAIN DRIFT LEXER
 *
 * This file is the main one of the Drift programming language lexer.
 ******************************************************************************/



/** Set of all symbols having 3 characters */
private val threeCharsSymbols = setOf(
    "..<",
)


/** Set of all symbols having 2 characters */
private val twoCharsSymbols = setOf(
    "==", "!=",
    "<=", ">=",
    "&&", "||",
    "->", "..",
    "><",
)


/** Set of all symbols having once character */
private val singleCharSymbols = setOf(
    '(', ')',
    '{', '}',
    '[', ']',
    '+', '-',
    '*', '/',
    '=', ':',
    '?', ',',
    '<', '>',
    '|', '&',
    '!', '.',
)



/**
 * This class contains all types of token
 */
sealed class Token {


    /**
     * Identifier token type.
     *
     * An identifier is a word that could
     * match with entity name or keyword.
     *
     * ```
     * keyword
     * ```
     */
    data class Identifier(val value: String) : Token() {
        /** @return If the identifier is the expected keyword */
        fun isKeyword(expected: Keyword): Boolean =
            this.value == expected.value
    }



    /**
     * String literal token type.
     *
     * It represents double-quoted words: a string.
     *
     * ```
     * "Hello, World!"
     * ```
     */
    data class StringLiteral(val value: String) : Token()



    /**
     * Numeric literal token type.
     *
     * It represents a numeric: Int, Int64, UInt, etc.
     */
    data class NumericLiteral(val value: String) : Token()



    /**
     * Boolean literal token type.
     *
     * It represents a boolean.
     *
     * ```
     * true
     * false
     * ```
     */
    data class BoolLiteral(val value: Boolean) : Token()



    /**
     * NULL literal token type.
     *
     * ```
     * null
     * ```
     */
    data object NullLiteral : Token()



    /**
     * Symbol token type.
     *
     * A symbol is a single or multi characters operator,
     * used to compute an expression like boolean comparison
     * or arithmetical equation, for example.
     *
     * ```
     * 1 + 1                                // Addition operator '+'
     * true ? thenBlock : elseBlock         // Ternary operators '?' and ':'
     * 1..9                                 // Range operator '..'
     * ```
     */
    data class Symbol(val value: String) : Token()



    /**
     * Keyword token type.
     *
     * A keyword is a reserved identifier, used to
     * execute native Drift statements.
     *
     * ```
     * fun
     * class
     * ```
     */
    enum class Keyword(val value: String) {
        IF("if"),
        ELSE("else"),
        FUNCTION("fun"),
        RETURN("return"),
        FOR("for"),
        CLASS("class"),
        IMMUTLET("let"),
        MUTLET("var"),
        AS("as"),
        LEAVE("leave"),
        STATIC("static"),
        IMPORT("import"),
    }



    /** Whitespace token type */
    data object Whitespace: Token()



    /** NewLine token type */
    data object NewLine : Token()



    /** End Of File (EOF) token type */
    data object EOF : Token()
}



/**
 * Main lexer function.
 *
 * This function tokenizes the provided source code.
 *
 * @param input Source code to lex
 * @return List of tokens
 * @throws DriftLexerException If a character is unexpected
 */
fun lex(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    var depth = 0

    while (i < input.length) {
        val c = input[i]

        if (c == '\r' && i + 1 < input.length && input[i + 1] == '\n') {
            if (depth == 0)
                tokens.add(Token.NewLine)

            i += 2
            continue
        } else if (c == '\n' || c == '\r') {
            if (depth == 0)
                tokens.add(Token.NewLine)

            i++
            continue
        }

        if (c.isWhitespace()) {
            tokens.add(Token.Whitespace)

            i++
            continue
        } else if (c == '"') {
            val (token, next) = lexString(input, i)
            tokens.add(token)

            i = next
            continue
        } else if (c.isDigit()) {
            val (token, next) = lexNumeric(input, i)
            tokens.add(token)

            i = next
            continue
        } else if (c.isLetter() || c == '_') {
            val (token, next) = lexWord(input, i)
            tokens.add(token)

            i = next
            continue
        } else if (input.substring(i).take(3) in threeCharsSymbols) {
            val (token, next) = lexSymbol(input, i, 3)
            tokens.add(token)

            i = next
            continue
        } else if (input.substring(i).take(2) in twoCharsSymbols) {
            val (token, next) = lexSymbol(input, i, 2)
            tokens.add(token)

            i = next
            continue
        } else if (c in singleCharSymbols) {
            val (token, next) = lexSymbol(input, i)
            tokens.add(token)

            i = next

            if (listOf('(', '[').contains(c))
                depth++
            else if (listOf(')', ']').contains(c))
                depth--

            continue
        } else {
            throw DriftLexerException("Unexpected character '$c'")
        }
    }

    tokens.add(Token.EOF)

    return tokens
}



/**
 * This function tokenizes a string expression
 *
 * @param input Source code
 * @param startIndex Start index of the expression to tokenize
 * @return A pair composed by the [Token.StringLiteral] object and the next
 * character position index
 * @throws DriftLexerException If the string expression is unterminated (unclosed)
 */
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



/**
 * This function tokenizes a numeric expression
 *
 * @param input Source code
 * @param startIndex Start index of the expression to tokenize
 * @return A pair composed by the [Token.IntLiteral] object and the next
 * character position index
 */
fun lexNumeric(input: String, startIndex: Int): Pair<Token, Int> {
    var i = startIndex

    while (i < input.length && input[i].isDigit()) {
        i++
    }

    val numberAsString = input.substring(startIndex, i)
    val token = Token.NumericLiteral(numberAsString)

    return token to i
}



/**
 * This function tokenizes a word.
 *
 * Null, booleans and identifiers are lexed by this function.
 *
 * @param input Source code
 * @param startIndex Start index of the expression to tokenize
 * @return A pair composed by the [Token] object and the next
 * character position index
 */
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



/**
 * This function tokenizes a symbol
 *
 * @param input Source code
 * @param startIndex Start index of the expression to tokenize
 * @param length Length of the symbol, by default = 1
 * @return A pair composed by the [Token.Symbol] object and the next
 * character position index
 */
fun lexSymbol(input: String, startIndex: Int, length: Int = 1): Pair<Token.Symbol, Int> {
    val symbol = input.substring(startIndex, startIndex + length)

    return Token.Symbol(symbol) to (startIndex + length)
}