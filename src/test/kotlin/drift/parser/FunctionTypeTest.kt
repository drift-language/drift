package drift.parser

import drift.ast.Function
import drift.ast.eval
import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.runtime.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionTypeTest {

    private fun parseFunctionFromSource(src: String) : Function {
        val tokens = lex(src)
        val statements = Parser(tokens).parse()
        val function = statements.first() as? Function

        assertNotNull(function)

        return function!!
    }

    @Test
    fun `Parse function with typed parameters`() {
        val function = parseFunctionFromSource("""
            fun add(x: Int, y: Int): Int { return x + y }
        """.trimIndent())

        assertEquals("add", function.name)
        assertEquals(IntType, function.returnType)
        assertEquals(2, function.parameters.size)

        assertEquals("x", function.parameters[0].name)
        assertEquals(IntType, function.parameters[0].type)

        assertEquals("y", function.parameters[1].name)
        assertEquals(IntType, function.parameters[1].type)
    }

    @Test
    fun `Function without explicit return type gets AnyType`() {
        val function = parseFunctionFromSource("""
            fun hello(name: String) { print(name) }
        """.trimIndent())

        assertEquals("hello", function.name)
        assertEquals(AnyType, function.returnType)
    }

    @Test
    fun `Function with Void return type`() {
        val function = parseFunctionFromSource("""
            fun log(msg: String): Void { print(msg) }
        """.trimIndent())
    }

    @Test
    fun `Function with wrong return type and expression`() {
        val code = """
            fun test(): String { return 1 }
            
            test()
        """.trimIndent()

        val tokens = lex(code)
        val statements = Parser(tokens).parse()
        val env = DrEnv()

        assertThrows<DriftTypeException> {
            for (statement in statements) {
                statement.eval(env)
            }
        }
    }

    @Test
    fun `Function with nullable String return type`() {
        val function = parseFunctionFromSource("""
            fun maybeName(): String? { return null }
        """.trimIndent())

        assertEquals("maybeName", function.name)
        assertEquals(OptionalType(StringType), function.returnType)
    }

    @Test
    fun `Function with union return type`() {
        val function = parseFunctionFromSource("""
            fun test1(): String|Int { return 2 }
        """.trimIndent())

        assertEquals("test1", function.name)
        assertEquals(UnionType(listOf(
            StringType,
            IntType,
        )), function.returnType)
    }

    @Test
    fun `Function with union type on a parameter`() {
        val function = parseFunctionFromSource("""
            fun test2(x: Int|String) { return x }
        """.trimIndent())

        assertEquals("test2", function.name)
        assertEquals(UnionType(listOf(
            IntType,
            StringType,
        )), function.parameters[0].type)
    }

    @Test
    fun `Function with union return type Int and Last must throw`() {
        assertThrows<DriftTypeException> {
            parseFunctionFromSource("""
                fun test(): Int|Last { return 1 }
                test()
            """.trimIndent())
        }
    }
}