package drift.ast

import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BlockTest {

    @Test
    fun `Parse and eval block with two print statements`() {
        val outputs = evalWithOutputs("""
            {
                test("Hello")
                test("World")
            }
        """.trimIndent())

        assertEquals(listOf("Hello", "World"), outputs)
    }
}
