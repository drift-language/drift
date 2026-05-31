package drift.parser

import drift.ast.statements.Block
import drift.ast.statements.For
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ForParserTest {

    private fun parse(code: String) = Parser(lex(code)).parse().first() as For


    @Nested
    inner class IterableTests {

        @Test
        fun `iterable expression is captured`() {
            val stmt = parse("for items {}")

            assertNotNull(stmt.iterable)
        }
    }


    @Nested
    inner class IterationVariableTests {

        @Test
        fun `no iteration variable`() {
            val stmt = parse("for items {}")

            assertTrue(stmt.variables.isEmpty())
        }

        @Test
        fun `single iteration variable with as`() {
            val stmt = parse("for items { as x\n}")

            assertEquals(1, stmt.variables.size)
            assertEquals("x", stmt.variables[0].name)
        }

        @Test
        fun `multiple iteration variables`() {
            val stmt = parse("for items { as x, y\n}")

            assertEquals(2, stmt.variables.size)
            assertAll(
                { assertEquals("x", stmt.variables[0].name) },
                { assertEquals("y", stmt.variables[1].name) },
            )
        }
    }


    @Nested
    inner class BodyTests {

        @Test
        fun `body is captured`() {
            val stmt = parse("for items {}")

            assertNotNull(stmt.body)
        }

        @Test
        fun `empty body has no statements`() {
            val stmt = parse("for items {}")

            assertTrue(stmt.body.statements.isEmpty())
        }
    }
}