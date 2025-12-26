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
import drift.utils.evalWithOutput
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ClassStaticTest {

    @Test
    fun `Empty static block should not throw`() {
        assertDoesNotThrow {
            evalProgram("""
                class A {
                    static { }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Static method with call`() {
        assertDoesNotThrow {
            val output = evalWithOutput("""
                class A {
                    static {
                        fun hello { test(1) }
                    }
                }
                
                A.hello()
            """.trimIndent())

            assertEquals("1", output)
        }
    }

    @Test
    fun `Static with some fields`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                class A {
                    static {
                        let count: Int = 0
                        var flag: Bool = false
                    }
                }
                
                test(A.count, A.flag)
            """.trimIndent())

            assertEquals(listOf("0", "false"), outputs)
        }
    }

    @Test
    fun `Invalid statement in static block must throw`() {
        assertThrows<DriftParserException> {
            evalProgram("""
                class A {
                    static { 123 }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Static field should not be accessible via instance getter stmt`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class A {
                    static {
                        let a = 1
                    }
                }
                
                A().a
            """.trimIndent())
        }
    }

    @Test
    fun `Static method should not be accessible via instance getter stmt`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class A {
                    static {
                        fun hello {}
                    }
                }
                
                A().hello()
            """.trimIndent())
        }
    }

    @Test
    fun `Same name for both static and dynamic fields must throw`() {
        assertThrows<DriftParserException> {
            evalProgram("""
                class A {
                    static {
                        let a = 1
                    }
                    
                    let a = 2
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Same name for both static and dynamic methods must throw`() {
        assertThrows<DriftParserException> {
            evalProgram("""
                class A {
                    static {
                        fun hello {}
                    }   
                    
                    fun hello {}
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Static mutable field modification must not throw`() {
        assertDoesNotThrow {
            val output = evalWithOutput("""
                class A {
                    static {
                        var a = 1
                    }
                }
                
                A.a = 2
                
                test(A.a)
            """.trimIndent())

            assertEquals("2", output)
        }
    }

    @Test
    fun `Static immutable field modification must throw`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class A {
                    static {
                        let a = 1
                    }
                }
                
                A.a = 2
            """.trimIndent())
        }
    }

    @Test
    fun `Class with both primary constructor and static block must not throw`() {
        assertDoesNotThrow {
            evalProgram("""
                class A(value: String) {
                    static {}
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Same name for static field and primary constructor field must throw`() {
        assertThrows<DriftParserException> {
            evalProgram("""
                class A(value: String) {
                    static {
                        let value: Int = 1
                    }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Static field initialization must be evaluated only on definition, not call`() {
        assertDoesNotThrow {
            val outputs = evalWithOutputs("""
                fun compute() : Int {
                    test("eval")
                    return 1 
                }
                
                class A {
                    static {
                        let a = compute()
                    }
                }
                
                test(A.a)
            """.trimIndent())

            assertEquals(listOf("eval", "1"), outputs)
        }
    }
}