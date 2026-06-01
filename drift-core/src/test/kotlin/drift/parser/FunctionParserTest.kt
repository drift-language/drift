package drift.parser

import drift.ast.statements.Func
import drift.lexer.lex
import drift.parser.exceptions.DPParameterAlreadyDefinedException
import drift.oldruntime.AnyType
import drift.oldruntime.ObjectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FunctionParserTest {

    private fun parse(code: String) = Parser(lex(code)).parse().first() as Func


    @Nested
    inner class NameTests {

        @Test
        fun `name is correctly captured`() {
            assertEquals("foo", parse("fun foo {}").name)
        }
    }


    @Nested
    inner class ParameterTests {

        @Test
        fun `no parameters`() {
            assertTrue(parse("fun foo {}").parameters.isEmpty())
        }

        @Test
        fun `single parameter with type`() {
            val params = parse("fun foo(x: Int) {}").parameters
            assertEquals(1, params.size)
            assertEquals("x", params[0].name)
            assertEquals(ObjectType("Int"), params[0].type)
        }

        @Test
        fun `multiple parameters`() {
            val params = parse("fun foo(x: Int, y: String) {}").parameters
            assertEquals(2, params.size)
            assertEquals("x", params[0].name)
            assertEquals("y", params[1].name)
        }

        @Test
        fun `parameter without type defaults to AnyType`() {
            val params = parse("fun foo(x) {}").parameters
            assertEquals(AnyType, params[0].type)
        }

        @Test
        fun `positional parameter marked with star`() {
            val params = parse("fun foo(*x: Int) {}").parameters
            assertTrue(params[0].isPositional)
        }

        @Test
        fun `non-positional parameter not marked with star`() {
            val params = parse("fun foo(x: Int) {}").parameters
            assertFalse(params[0].isPositional)
        }

        @Test
        fun `parameter with default value`() {
            val params = parse("fun foo(x: Int = 0) {}").parameters
            assertNotNull(params[0].defaultValue)
        }

        @Test
        fun `duplicate parameter name throws`() {
            assertThrows<DPParameterAlreadyDefinedException> {
                parse("fun foo(x: Int, x: String) {}")
            }
        }
    }


    @Nested
    inner class ReturnTypeTests {

        @Test
        fun `no return type defaults to AnyType`() {
            assertEquals(AnyType, parse("fun foo {}").returnType)
        }

        @Test
        fun `explicit return type`() {
            assertEquals(ObjectType("Int"), parse("fun foo: Int {}").returnType)
        }

        @Test
        fun `return type with parameters`() {
            assertEquals(ObjectType("Int"), parse("fun foo(x: Int): Int {}").returnType)
        }
    }
}