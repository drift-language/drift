package drift.ast

import drift.lexer.Token
import drift.lexer.exceptions.DLUnexpectedCharacterException
import drift.lexer.exceptions.DLUnterminatedStringLiteralException
import drift.lexer.lex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SymbolsTest {

    @Test
    fun `Recognizes simple symbols`() {
        val code = "+ - * / == != <= >= ? : ."
        val tokens = lex(code)
        val expected = listOf(
            Token.Symbol("+"),
            Token.Symbol("-"),
            Token.Symbol("*"),
            Token.Symbol("/"),
            Token.Symbol("=="),
            Token.Symbol("!="),
            Token.Symbol("<="),
            Token.Symbol(">="),
            Token.Symbol("?"),
            Token.Symbol(":"),
            Token.Symbol("."),
        )

        assertEquals(expected, tokens.filterIsInstance<Token.Symbol>())
    }

    @Test
    fun `Recognizes newline LF and CRLF`() {
        val code = "let x = 1\nlet y = 2\r\nlet z = 3"
        val tokens = lex(code)
        val newlines = tokens.filterIsInstance<Token.NewLine>()

        assertEquals(2, newlines.size)
    }

    @Test
    fun `Recognizes identifiers and literals`() {
        val code = "let name = \"John\" let age = 42"
        val tokens = lex(code)

        assertTrue(tokens.any { it is Token.Identifier && it.value == "name" })
        assertTrue(tokens.any { it is Token.StringLiteral && it.value == "John" })
        assertTrue(tokens.any { it is Token.NumericLiteral && it.value == "42" })
    }

    @Test
    fun `Handles unknown character gracefully`() {
        val code = "@"

        assertThrows<DLUnexpectedCharacterException> {
            lex(code)
        }
    }

    @Test
    fun `Handles unterminated string literal`() {
        val code = "\"hello"

        assertThrows<DLUnterminatedStringLiteralException> {
            lex(code)
        }
    }

    @Test
    fun `Handles dot symbol correctly for property access`() {
        val code = "user.name"
        val tokens = lex(code)

        assertTrue(tokens.any { it is Token.Symbol && it.value == "." })
    }
}