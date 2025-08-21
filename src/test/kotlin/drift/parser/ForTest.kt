/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser

import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.utils.evalProgram
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ForTest {

    @Test
    fun `For using range, without variable and _ usage`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for 1..3 {
                    test("Hello")
                }
            """.trimIndent())

            assertEquals(outputs, List(3) { "Hello" })
        }
    }

    @Test
    fun `For using range, without variable, using _ variable`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for 1..3 {
                    test(_)
                }
            """.trimIndent())

            assertEquals(outputs, listOf("1", "2", "3"))
        }
    }

    @Test
    fun `For using range, with variable`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for 1..3 { as i 
                    test(i)
                }
            """.trimIndent())

            assertEquals(outputs, listOf("1", "2", "3"))
        }
    }

    @Test
    fun `For using range, with many variables must throw`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                for 1..3 { as a, b
                    print(a, b)
                }
            """.trimIndent())
        }
    }



    @Test
    fun `For using list, without variable and _ usage`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for [1, 2, 3] {
                    test("Hello")
                }
            """.trimIndent())

            assertEquals(outputs, List(3) { "Hello" })
        }
    }

    @Test
    fun `For using list, without variable, using _ variable`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for [1, 2, 3] {
                    test(_)
                }
            """.trimIndent())

            assertEquals(outputs, listOf("1", "2", "3"))
        }
    }

    @Test
    fun `For using list, with variable`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for [1, 2, 3] { as value 
                    test(value)
                }
            """.trimIndent())

            assertEquals(outputs, listOf("1", "2", "3"))
        }
    }

    @Test
    fun `For using list, with 2 variables (index and value) must throw`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                for [1, 2, 3] { as value, i
                    test(i, value)
                }
            """.trimIndent())

            assertEquals(outputs, listOf(
                "0", "1",
                "1", "2",
                "2", "3",
            ))
        }
    }

    @Test
    fun `For using list, with more than 2 variables must throw`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                for [1, 2, 3] { as a, b, c
                    test(a, b, c)
                }
            """.trimIndent())
        }
    }
}