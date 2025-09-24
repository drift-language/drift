/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser

import drift.ast.*
import drift.exceptions.DriftParserException
import drift.parser.statements.parseStatement


/******************************************************************************
 * MAIN DRIFT PARSER
 *
 * This file is the main one of the Drift programming language parser.
 ******************************************************************************/



/**
 * Main parser class.
 *
 * The parser constructs an Abstract Syntax Tree (AST)
 * by using the lexer ([lex]) tokens.
 */
class Parser(
    /** Provided lexer tokens */
    private val tokens: List<Token>) {



    /** Current token index */
    private var i = 0


    /**
     * Current depth level.
     *
     * Tracks the current nesting level of parentheses, brackets and braces.
     * Incremented on encountering '(', '[', or '{', and decremented on ')', ']', or '}'.
     * Used to manage parsing context and handle newlines within nested structures.
     */
    private var depth = 0



    /**
     * Operators priority ranking.
     *
     * All operators are ranked respecting their priority
     * on evaluation.
     *
     * From less to most high priority.
     */
    internal val operatorPrecedence: Map<String, Int> = mapOf(
        "="     to 1,
        ".."    to 2,

        "?"     to 3,

        "=="    to 4,
        "!="    to 4,
        ">"     to 4,
        "<"     to 4,
        ">="    to 4,
        "<="    to 4,

        "+"     to 5,
        "-"     to 5,

        "*"     to 6,
        "/"     to 6,

        "&&"    to 7,

        "||"    to 8,
    )



    /** @return Current token */
    internal fun current() : Token? = tokens.getOrNull(i)



    /**
     * Go to the next token by incrementing i + 1.
     *
     * @param ignoreNewLines If newlines must be ignored
     * until next token
     * @param ignoreWhitespaces If whitespaces must be ignored
     * until next token
     */
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
        } else if (ignoreNewLines && c is Token.NewLine && depth > 0) {
            advance()
        }
    }



    /** @return If the current token is [Token.EOF], end of file */
    internal fun isAtEnd() : Boolean = current() == Token.EOF



    /**
     * Attempt to parse the whole file by looping
     * on each top-level statement and evaluating each one.
     *
     * @return The entire list of statements (AST)
     * @throws DriftParserException If two statements are not separated
     * by a newline on top-level
     */
    fun parse(): List<DrStmt> {
        val statements = mutableListOf<DrStmt>()

        while (!isAtEnd()) {
            skip(Token.NewLine)

            val statement = parseStatement()
            statements.add(statement)

            val next = current()

            if (next is Token.NewLine) {
                advance()
            } else if (!isAtEnd()) {
                throw DriftParserException("Expected newline after top-level statement but found $next")
            }
        }

        return statements
    }



    /**
     * Check if the current token is the same symbol
     * that the provided one.
     *
     * This method does not advance.
     *
     * @param value Searched symbol
     * @return If the current token is the searched symbol
     */
    internal fun checkSymbol(value: String) : Boolean {
        val token = current()

        return token is Token.Symbol && token.value == value
    }



    /**
     * Check if the current token is the same symbol
     * that the provided one.
     *
     * This method advances to the next token if
     * the token corresponds.
     *
     * @param value Searched symbol
     * @return If the current token is the searched symbol
     */
    internal fun matchSymbol(value: String) : Boolean {
        val token = current()

        if (token is Token.Symbol && token.value == value) {
            advance(false)

            return true
        }

        return false
    }



    /**
     * Check if the next token is the same symbol
     * that the provided one.
     *
     * This method does not advance.
     *
     * @param value Searched symbol
     * @param ignoreNewLines If newlines must be ignored
     * until next token
     * @param ignoreWhitespaces If whitespaces must be ignored
     * until next token
     * @return If the next token is the searched symbol
     */
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



    /**
     * Expect the provided token type on the current token.
     *
     * This method does not advance.
     *
     * @param T Expected token type
     * @param message Custom error message beginning
     * @return Expected token object if the search is successful
     * @throws DriftParserException If the expected symbol type does
     * not match with current token one
     */
    internal inline fun <reified T : Token> expect(message: String) : T {
        val token = current()

        return token as? T ?: throw DriftParserException("$message, but found $token")
    }



    /**
     * Expect the provided symbol on current token.
     *
     * If found, an implicit advance — ignoring newlines —
     * is done
     *
     * @param expected Expected symbol
     * @throws DriftParserException If the expected symbol is
     * not found
     */
    internal fun expectSymbol(expected: String, advanceOnSuccess: Boolean = true) {
        val token = current()

        if (token !is Token.Symbol || token.value != expected) {
            throw DriftParserException("Expected '$expected' but found $token")
        }

        if (advanceOnSuccess)
            advance(false)
    }



    /** @return If the following expression is a lambda function */
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



    /**
     * Skip all direct future token matching the provided one.
     *
     * This method will advance until the next token that is
     * not matching with expected one.
     */
    internal fun skip(token: Token) {
        while (current() == token)
            advance()
    }
}