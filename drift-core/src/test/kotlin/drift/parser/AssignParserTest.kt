package drift.parser

import drift.ast.expressions.Assign
import drift.ast.expressions.Literal
import drift.ast.expressions.Reference
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AssignParserTest {

    private fun parseAssign(code: String) =
        (Parser(lex(code)).parse().first() as ExprStmt).expr as Assign


    @Nested
    inner class NameTests {

        @Test
        fun `variable name is captured`() {
            val assign = parseAssign("x = 5")
            assertEquals("x", assign.name)
        }
    }


    @Nested
    inner class ValueTests {

        @Test
        fun `value expression is captured`() {
            val assign = parseAssign("x = 5")
            assertNotNull(assign.value)
        }

        @Test
        fun `literal value`() {
            val assign = parseAssign("x = 5")
            assertTrue(assign.value is Literal)
        }

        @Test
        fun `variable value`() {
            val assign = parseAssign("x = y")
            assertTrue(assign.value is Reference)
        }

        @Test
        fun `expression value`() {
            val assign = parseAssign("x = 1 + 2")
            assertNotNull(assign.value)
        }
    }
}
