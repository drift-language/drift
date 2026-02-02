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
import drift.runtime.values.containers.range.ParserExclusiveRange
import drift.runtime.values.containers.range.ParserInclusiveRange
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNull
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BinaryExprTest {

    private fun evalExpr(input: String) : ParserValue {
        val env = DrEnv()
        env.apply {
            defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Int64", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("String", ParserClass("String", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
        }
        val ast = Parser(lex(input)).parse()


        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        var result: ParserValue = ParserNull

        ast.forEach {
            result = it.eval(env)
        }

        return result
    }

    @Test
    fun `Test simple addition`() {
        val result = evalExpr("1 + 2")

        assertEquals(ParserInt(3), result)
    }

    @Test
    fun `Test simple concatenation`() {
        val result = evalExpr("\"a\" + \"b\"")

        assertEquals(ParserString("ab"), result)
    }

    @Test
    fun `Test mixed precedence 1 ADD 2 MUL 3`() {
        val result = evalExpr("1 + 2 * 3")

        assertEquals(ParserInt(7), result)
    }

    @Test
    fun `Test parenthesized expression`() {
        val result = evalExpr("(1 + 2) * 3")

        assertEquals(ParserInt(9), result)
    }

    @Test
    fun `Test equality true`() {
        val result = evalExpr("1 + 1 == 2")

        assertEquals(ParserBool(true), result)
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

            assertEquals(range, ParserInclusiveRange(ParserInt(1), ParserInt(3)))
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

            assertEquals(range, ParserInclusiveRange(ParserInt64(1), ParserInt64(3)))
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

            assertEquals(range, ParserExclusiveRange(ParserInt(1), ParserInt(3)))
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

            assertEquals(range, ParserExclusiveRange(ParserInt64(1), ParserInt64(3)))
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