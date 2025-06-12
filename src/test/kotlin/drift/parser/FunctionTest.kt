package drift.parser

import drift.runtime.DrEnv
import drift.runtime.DrInt
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
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
}