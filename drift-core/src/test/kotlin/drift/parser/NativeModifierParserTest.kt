package drift.parser

import drift.ast.statements.Func
import drift.ast.statements.modifiers.NativeModifier
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NativeModifierParserTest {

    private fun parseFunc(code: String) = Parser(lex(code)).parse().first() as Func


    @Nested
    inner class ModifierPresenceTests {

        @Test
        fun `native function has NativeModifier`() {
            val func = parseFunc("native fun f()")
            assertTrue(func.modifiers.contains(NativeModifier))
        }

        @Test
        fun `regular function has no NativeModifier`() {
            val func = parseFunc("fun f() {}")
            assertFalse(func.modifiers.contains(NativeModifier))
        }
    }


    @Nested
    inner class BodyTests {

        @Test
        fun `native function has no body`() {
            val func = parseFunc("native fun f()")
            assertTrue(func.body.statements.isEmpty())
        }

        @Test
        fun `native function with body throws`() {
            assertThrows<Exception> {
                parseFunc("native fun f() {}")
            }
        }
    }


    @Nested
    inner class NameTests {

        @Test
        fun `native function name is captured`() {
            val func = parseFunc("native fun greet()")
            assertEquals("greet", func.name)
        }
    }


    @Nested
    inner class ParameterTests {

        @Test
        fun `native function with no parameters`() {
            val func = parseFunc("native fun f()")
            assertTrue(func.parameters.isEmpty())
        }

        @Test
        fun `native function with parameters`() {
            val func = parseFunc("native fun add(a: Int, b: Int)")
            assertEquals(2, func.parameters.size)
        }
    }
}
