/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime

import drift.oldruntime.exceptions.*
import drift.utils.evalProgram
import drift.utils.evalWithOutput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CallTest {

    @Test
    fun `Missing argument must throw`() {
        assertThrows<DRMissingArgumentException> {
            evalProgram("""
                fun f(a: Int, b: Int) {}
                f(1)
            """.trimIndent())
        }
    }

    @Test
    fun `Duplicate named argument must throw`() {
        assertThrows<DRTooManyArgumentsException> {
            evalProgram("""
                fun f(a: Int) {}
                f(a = 1, a = 2)
            """.trimIndent())
        }
    }

    @Test
    fun `Positional and named binding same parameter must throw`() {
        assertThrows<DRArgumentAlreadyBoundException> {
            evalProgram("""
                fun f(a: Int, b: Int) {}
                f(1, a = 2)
            """.trimIndent())
        }
    }

    @Test
    fun `Default argument must be applied`() {
        val result = evalWithOutput("""
            fun f(a: Int, b: Int = 2) { return a + b }
            test(f(1))
        """.trimIndent())

        assertEquals("3", result)
    }

    @Test
    fun `Default argument overridden by named`() {
        val result = evalWithOutput("""
            fun f(a: Int, b: Int = 2) { return a + b }
            test(f(1, b = 5))
        """.trimIndent())

        assertEquals("6", result)
    }

    @Test
    fun `Default argument overridden by positional`() {
        val result = evalWithOutput("""
            fun f(a: Int, b: Int = 2) { return a + b }
            test(f(1, 5))
        """.trimIndent())

        assertEquals("6", result)
    }

    @Test
    fun `Default argument must not see call site variables`() {
        assertThrows<DRVariableNotDefinedException> {
            evalProgram("""
                let x = 10
                fun f(a: Int = x) { return a }
                f()
            """.trimIndent())
        }
    }

    @Test
    fun `Calling non callable must throw`() {
        assertThrows<DRNonCallableInvocationException> {
            evalProgram("""
                let x = 5
                x()
            """.trimIndent())
        }
    }

    @Test
    fun `Return type mismatch must throw`() {
        assertThrows<DRUnsuccessfulCastException> {
            evalProgram("""
                fun f(): Int { return "hello" }
                f()
            """.trimIndent())
        }
    }

    @Test
    fun `Function with no parameters can be called`() {
        val result = evalWithOutput("""
            fun f { return 42 }
            test(f())
        """.trimIndent())

        assertEquals("42", result)
    }

    @Test
    fun `use Void function return value must throw`() {
        assertThrows<DRCannotUseVoidAsValueException> {
            evalWithOutput("""
                fun f {}
                test(f())
            """.trimIndent())
        }
    }
}