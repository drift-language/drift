package drift.parser

import drift.runtime.DrEnv
import drift.runtime.DrInt
import drift.runtime.DrNull
import drift.runtime.DrVoid
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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
}