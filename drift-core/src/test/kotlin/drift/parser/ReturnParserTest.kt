package drift.parser

import drift.ast.expressions.Literal
import drift.ast.expressions.Reference
import drift.ast.statements.Return
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReturnParserTest {

    private fun parseReturn(code: String) =
        Parser(lex(code)).parse().first() as Return


    @Nested
    inner class ValueTests {

        @Test
        fun `return with literal value`() {
            val ret = parseReturn("return 42")
            assertTrue(ret.value is Literal)
        }

        @Test
        fun `return with variable`() {
            val ret = parseReturn("return x")
            assertTrue(ret.value is Reference)
        }

        @Test
        fun `leave keyword produces void return`() {
            val ret = parseReturn("leave")
            assertNull((ret).value)
        }
    }
}
