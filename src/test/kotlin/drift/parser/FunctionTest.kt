package drift.parser

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrEnv
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.specials.DrVoid
import drift.utils.evalProgram
import drift.utils.evalWithOutput
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class FunctionTest {

    @Test
    fun `Function returns value correctly`() {
        val result = evalProgram("""
            fun double(*x) {
                return x * 2
            }
            
            double(x = 5)
        """.trimIndent())

        assertEquals(DrInt(10), result)
    }

    @Test
    fun `Function without parameter`() {
        val result = evalProgram("""
            fun test() {}
            
            test()
        """.trimIndent())

        assertEquals(DrVoid, result)
    }

    @Test
    fun `Function with one parameter`() {
        val result = evalProgram("""
            fun test(x) {}
            
            test(x = 1)
        """.trimIndent())

        assertEquals(DrVoid, result)
    }

    @Test
    fun `Function with multiple parameters`() {
        val result = evalProgram("""
            fun test(x, y) {}
            
            test(x = 1, y = 2)
        """.trimIndent())

        assertEquals(DrVoid, result)
    }

    @Test
    fun `Function without parameters and parenthesis`() {
        val result = evalProgram("""
            fun test {
                return 1
            }
            
            test()
        """.trimIndent())

        assertEquals(DrInt(1), result)
    }

    @Test
    fun `Function with explicit return type without return statement must throw`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                fun test : Int { }
                test()
            """.trimIndent())
        }
    }

    @Test
    fun `Function with TRUE take-if structure must return value`() {
        assertDoesNotThrow {
            val output = evalWithOutput("""
                fun a : Int? {
                    return true ? 123
                }
                
                test(a())
            """.trimIndent())

            assertEquals(output, "123")
        }
    }

    @Test
    fun `Function with FALSE take-if structure must return NULL`() {
        assertDoesNotThrow {
            val output = evalWithOutput("""
                fun a : Int? {
                    return false ? 123
                }
                
                test(a())
            """.trimIndent())

            assertEquals(output, "null")
        }
    }

    @Test
    fun `Lambda must use environment values dynamically`() {
        val l = evalWithOutputs("""
            var a = 1
            fun b : Last {a}
            test(b())
            a = 2
            test(b())
        """.trimIndent())

        assertEquals(listOf("1", "2"), l)
    }
}