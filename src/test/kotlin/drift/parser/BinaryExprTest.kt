package drift.parser

import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.runtime.*
import drift.runtime.values.containers.DrRange
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrNull
import drift.utils.evalProgram
import drift.utils.evalWithOutput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BinaryExprTest {

    private fun evalExpr(input: String) : DrValue {
        val env = DrEnv().apply {
            defineClass("Int", DrClass("Int", emptyList(), emptyList()))
            defineClass("Int64", DrClass("Int", emptyList(), emptyList()))
            defineClass("String", DrClass("String", emptyList(), emptyList()))
            defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
        }
        val ast = Parser(lex(input)).parse()

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        var result: DrValue = DrNull

        ast.forEach {
            result = it.eval(env)
        }

        return result
    }

    @Test
    fun `Test simple addition`() {
        val result = evalExpr("1 + 2")

        assertEquals(DrInt(3), result)
    }

    @Test
    fun `Test simple concatenation`() {
        val result = evalExpr("\"a\" + \"b\"")

        assertEquals(DrString("ab"), result)
    }

    @Test
    fun `Test mixed precedence 1 ADD 2 MUL 3`() {
        val result = evalExpr("1 + 2 * 3")

        assertEquals(DrInt(7), result)
    }

    @Test
    fun `Test parenthesized expression`() {
        val result = evalExpr("(1 + 2) * 3")

        assertEquals(DrInt(9), result)
    }

    @Test
    fun `Test equality true`() {
        val result = evalExpr("1 + 1 == 2")

        assertEquals(DrBool(true), result)
    }

    @Test
    fun `Division by zero should throw`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                let x = 1 / 0
            """.trimIndent())
        }
    }

    @Test
    fun `Test range with both Int`() {
        assertDoesNotThrow {
            val range = evalExpr("1..3")

            assertEquals(range, DrRange(DrInt(1), DrInt(3)))
        }
    }

    @Test
    fun `Test range with both Int64`() {
        assertDoesNotThrow {
            val range = evalExpr("""
                let a: Int64 = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())

            assertEquals(range, DrRange(DrInt64(1), DrInt64(3)))
        }
    }

    @Test
    fun `Test range with unsigned integer must throw`() {
        assertThrows<DriftTypeException> {
            evalExpr("""
                let a: UInt = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())
        }
    }

    @Test
    fun `Test range with both different integer types`() {
        assertThrows<DriftRuntimeException> {
            evalExpr("""
                let a: Int = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())
        }
    }
}