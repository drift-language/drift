package drift.parser

import drift.ast.expressions.Array
import drift.ast.expressions.Literal
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ArrayParserTest {

    private fun parseArray(code: String) =
        (Parser(lex(code)).parse().first() as ExprStmt).expr as Array


    @Nested
    inner class SizeTests {

        @Test
        fun `empty array has no elements`() {
            val array = parseArray("[]")
            assertTrue(array.values.isEmpty())
        }

        @Test
        fun `single element array`() {
            val array = parseArray("[1]")
            assertEquals(1, array.values.size)
        }

        @Test
        fun `multiple elements array`() {
            val array = parseArray("[1, 2, 3]")
            assertEquals(3, array.values.size)
        }
    }


    @Nested
    inner class ElementTests {

        @Test
        fun `elements are captured as expressions`() {
            val array = parseArray("[1, 2]")
            assertTrue(array.values[0] is Literal)
            assertTrue(array.values[1] is Literal)
        }

        @Test
        fun `multiline array is accepted`() {
            val array = parseArray("[\n1,\n2\n]")
            assertEquals(2, array.values.size)
        }
    }
}
