package drift.parser

import drift.ast.expressions.Call
import drift.ast.expressions.Variable
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CallParserTest {

    private fun parseCall(code: String) =
        (Parser(lex(code)).parse().first() as ExprStmt).expr as Call


    @Nested
    inner class CalleeTests {

        @Test
        fun `callee is captured`() {
            val call = parseCall("foo()")
            assertTrue(call.callee is Variable)
        }

        @Test
        fun `callee name is correct`() {
            val call = parseCall("foo()")
            assertEquals("foo", (call.callee as Variable).name)
        }
    }


    @Nested
    inner class ArgumentCountTests {

        @Test
        fun `call with no arguments`() {
            val call = parseCall("foo()")
            assertTrue(call.args.isEmpty())
        }

        @Test
        fun `call with one argument`() {
            val call = parseCall("foo(1)")
            assertEquals(1, call.args.size)
        }

        @Test
        fun `call with multiple arguments`() {
            val call = parseCall("foo(1, 2, 3)")
            assertEquals(3, call.args.size)
        }
    }


    @Nested
    inner class PositionalArgumentTests {

        @Test
        fun `positional argument has null name`() {
            val call = parseCall("foo(1)")
            assertNull(call.args[0].name)
        }

        @Test
        fun `positional argument expression is captured`() {
            val call = parseCall("foo(1)")
            assertNotNull(call.args[0].expr)
        }
    }


    @Nested
    inner class NamedArgumentTests {

        @Test
        fun `named argument name is captured`() {
            val call = parseCall("foo(x = 1)")
            assertEquals("x", call.args[0].name)
        }

        @Test
        fun `named argument expression is captured`() {
            val call = parseCall("foo(x = 1)")
            assertNotNull(call.args[0].expr)
        }

        @Test
        fun `mixed positional and named arguments`() {
            val call = parseCall("foo(1, x = 2)")
            assertNull(call.args[0].name)
            assertEquals("x", call.args[1].name)
        }
    }
}
