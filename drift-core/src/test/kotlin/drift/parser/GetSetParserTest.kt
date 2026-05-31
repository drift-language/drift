package drift.parser

import drift.ast.expressions.Get
import drift.ast.expressions.Set
import drift.ast.expressions.Variable
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetSetParserTest {

    private fun parseExpr(code: String) =
        (Parser(lex(code)).parse().first() as ExprStmt).expr


    @Nested
    inner class GetTests {

        @Test
        fun `property access produces Get node`() {
            val expr = parseExpr("obj.field")
            assertTrue(expr is Get)
        }

        @Test
        fun `receiver is captured`() {
            val get = parseExpr("obj.field") as Get
            assertTrue(get.receiver is Variable)
        }

        @Test
        fun `receiver name is correct`() {
            val get = parseExpr("obj.field") as Get
            assertEquals("obj", (get.receiver as Variable).name)
        }

        @Test
        fun `property name is captured`() {
            val get = parseExpr("obj.field") as Get
            assertEquals("field", get.name)
        }

        @Test
        fun `chained property access`() {
            val get = parseExpr("a.b.c") as Get
            assertEquals("c", get.name)
            assertTrue(get.receiver is Get)
            assertEquals("b", (get.receiver as Get).name)
        }
    }


    @Nested
    inner class SetTests {

        @Test
        fun `property assignment produces Set node`() {
            val expr = parseExpr("obj.field = 5")
            assertTrue(expr is Set)
        }

        @Test
        fun `receiver is captured`() {
            val set = parseExpr("obj.field = 5") as Set
            assertTrue(set.receiver is Variable)
        }

        @Test
        fun `property name is captured`() {
            val set = parseExpr("obj.field = 5") as Set
            assertEquals("field", set.name)
        }

        @Test
        fun `value expression is captured`() {
            val set = parseExpr("obj.field = 5") as Set
            assertNotNull(set.value)
        }
    }
}
