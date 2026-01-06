/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser

import drift.runtime.evaluators.eval
import drift.checkers.collectors.SymbolCollector
import drift.checkers.TypeChecker
import drift.lexer.lex
import drift.runtime.*
import drift.runtime.exceptions.DRDivisionByZeroException
import drift.runtime.exceptions.DRUnsupportedOperatorException
import drift.runtime.values.containers.range.DrExclusiveRange
import drift.runtime.values.containers.range.DrInclusiveRange
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrNull
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BinaryExprTest {

    private fun evalExpr(input: String) : DrValue {
        val env = DrEnv()
        env.apply {
            defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Int64", DrClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("String", DrClass("String", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
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
        assertThrows<DRDivisionByZeroException> {
            evalProgram("""
                let x = 1 / 0
            """.trimIndent())
        }
    }

    @Test
    fun `Test inclusive range with both Int`() {
        assertDoesNotThrow {
            val range = evalExpr("1..3")

            assertEquals(range, DrInclusiveRange(DrInt(1), DrInt(3)))
        }
    }

    @Test
    fun `Test inclusive range with both Int64`() {
        assertDoesNotThrow {
            val range = evalExpr("""
                let a: Int64 = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())

            assertEquals(range, DrInclusiveRange(DrInt64(1), DrInt64(3)))
        }
    }

    @Test
    fun `Test inclusive range with unsigned integer must throw`() {
        assertThrows<DRUnsupportedOperatorException> {
            evalExpr("""
                let a: UInt = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())
        }
    }

    @Test
    fun `Test inclusive range with both different integer types`() {
        assertThrows<DRUnsupportedOperatorException> {
            evalExpr("""
                let a: Int = 1
                let b: Int64 = 3
                
                a..b
            """.trimIndent())
        }
    }


    @Test
    fun `Test exclusive range with both Int`() {
        assertDoesNotThrow {
            val range = evalExpr("1..<3")

            assertEquals(range, DrExclusiveRange(DrInt(1), DrInt(3)))
        }
    }

    @Test
    fun `Test exclusive range with both Int64`() {
        assertDoesNotThrow {
            val range = evalExpr("""
                let a: Int64 = 1
                let b: Int64 = 3
                
                a..<b
            """.trimIndent())

            assertEquals(range, DrExclusiveRange(DrInt64(1), DrInt64(3)))
        }
    }

    @Test
    fun `Test exclusive range with unsigned integer must throw`() {
        assertThrows<DRUnsupportedOperatorException> {
            evalExpr("""
                let a: UInt = 1
                let b: Int64 = 3
                
                a..<b
            """.trimIndent())
        }
    }

    @Test
    fun `Test exclusive range with both different integer types`() {
        assertThrows<DRUnsupportedOperatorException> {
            evalExpr("""
                let a: Int = 1
                let b: Int64 = 3
                
                a..<b
            """.trimIndent())
        }
    }
}