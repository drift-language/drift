package drift.parser

import drift.ast.statements.Func
import drift.lexer.lex
import drift.parser.exceptions.DPSpecialInUnionTypeException
import drift.runtime.*
import drift.runtime.exceptions.DRUnsuccessfulCastException
import drift.utils.evalProgram
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class FuncTypeTest {

    private fun parseFunctionFromSource(src: String) : Func {
        val tokens = lex(src)
        val statements = Parser(tokens).parse()
        val func = statements.first() as? Func

        assertNotNull(func)

        return func!!
    }

    @Test
    fun `Parse function with typed parameters`() {
        val function = parseFunctionFromSource("""
            fun add(x: Int, y: Int): Int { return x + y }
        """.trimIndent())

        assertEquals("add", function.name)
        assertEquals(ObjectType("Int"), function.returnType)
        assertEquals(2, function.parameters.size)

        assertEquals("x", function.parameters[0].name)
        assertEquals(ObjectType("Int"), function.parameters[0].type)

        assertEquals("y", function.parameters[1].name)
        assertEquals(ObjectType("Int"), function.parameters[1].type)
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
        assertThrows<DRUnsuccessfulCastException> {
            evalProgram("""
                fun test(): String { return 1 }
                
                test()
            """.trimIndent())
        }
    }

    @Test
    fun `Function with nullable String return type`() {
        val function = parseFunctionFromSource("""
            fun maybeName(): String? { return null }
        """.trimIndent())

        assertEquals("maybeName", function.name)
        assertEquals(OptionalType(ObjectType("String")), function.returnType)
    }

    @Test
    fun `Function with union return type`() {
        val function = parseFunctionFromSource("""
            fun test1(): String|Int { return 2 }
        """.trimIndent())

        assertEquals("test1", function.name)
        assertEquals(UnionType(listOf(
            ObjectType("String"),
            ObjectType("Int"),
        )), function.returnType)
    }

    @Test
    fun `Function with union type on a parameter`() {
        val function = parseFunctionFromSource("""
            fun test2(x: Int|String) { return x }
        """.trimIndent())

        assertEquals("test2", function.name)
        assertEquals(UnionType(listOf(
            ObjectType("Int"),
            ObjectType("String"),
        )), function.parameters[0].type)
    }

    @Test
    fun `Function with union return type Int and Last must throw`() {
        assertThrows<DPSpecialInUnionTypeException> {
            parseFunctionFromSource("""
                fun test(): Int|Last { return 1 }
                test()
            """.trimIndent())
        }
    }
}