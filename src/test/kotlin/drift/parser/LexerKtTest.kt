package drift.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LexerKtTest {

    @Test
    fun `Test string literal`() {
        val tokens = lex("print(\"Hello World\")")

        assertEquals(
            listOf(
                Token.Identifier("print"),
                Token.Symbol("("),
                Token.StringLiteral("Hello World"),
                Token.Symbol(")"),
                Token.EOF
            ),
            tokens
        )
    }

    @Test
    fun `Test integer literal`() {
        val tokens = lex("add(1, 23)")

        assertEquals(
            listOf(
                Token.Identifier("add"),
                Token.Symbol("("),
                Token.NumericLiteral("1"),
                Token.Symbol(","),
                Token.Whitespace,
                Token.NumericLiteral("23"),
                Token.Symbol(")"),
                Token.EOF
            ),
            tokens
        )
    }

    @Test
    fun `Test boolean literals`() {
        val tokens = lex("true false")

        assertEquals(
            listOf(
                Token.BoolLiteral(true),
                Token.Whitespace,
                Token.BoolLiteral(false),
                Token.EOF
            ),
            tokens
        )
    }

    @Test
    fun `Test mixed symbols`() {
        val tokens = lex("{x = 1 + 2}")

        assertEquals(
            listOf(
                Token.Symbol("{"),
                Token.Identifier("x"),
                Token.Whitespace,
                Token.Symbol("="),
                Token.Whitespace,
                Token.NumericLiteral("1"),
                Token.Whitespace,
                Token.Symbol("+"),
                Token.Whitespace,
                Token.NumericLiteral("2"),
                Token.Symbol("}"),
                Token.EOF
            ),
            tokens
        )
    }
}