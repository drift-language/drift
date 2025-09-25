package drift.parser

import drift.exceptions.DriftParserException
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LambdaTest {

    @Test
    fun `Lambda without parameter`() {
        val l = evalWithOutputs("""
            test(() -> { return 42 } ())
        """.trimIndent())

        assertEquals(listOf("42"), l)
    }

    @Test
    fun `Lambda with one implicitly typed parameter`() {
        val l = evalWithOutputs("""
            test((x) -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter`() {
        val l = evalWithOutputs("""
            test((x: Int) -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with two implicitly typed parameters`() {
        val l = evalWithOutputs("""
            test((x, y) -> { return x + y } (1, 2))
        """.trimIndent())

        assertEquals(listOf("3"), l)
    }

    @Test
    fun `Lambda with two explicitly typed parameters`() {
        val l = evalWithOutputs("""
            test((x: Int, y: Int) -> { return x + y } (1, 2))
        """.trimIndent())

        assertEquals(listOf("3"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter and union return type`() {
        val l = evalWithOutputs("""
            test((x: Int): Int|String -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly union typed parameter and return type`() {
        val l = evalWithOutputs("""
            test((x: Int|String): Int -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly optional typed parameter and return type`() {
        val l = evalWithOutputs("""
            test((x: Int?): Int -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter and optional return type`() {
        val l = evalWithOutputs("""
            test((x: Int): Int? -> { return null } (1))
        """.trimIndent())

        assertEquals(listOf("null"), l)
    }

    @Test
    fun `Lambda with Last special return type`() {
        val l = evalWithOutputs("""
            test((): Last -> { 1 } ())
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with same parameter defined two times must throw exception`() {
        assertThrows<DriftParserException> {
            evalWithOutputs("""
                test((x, x) -> { return x } ())
            """.trimIndent())
        }
    }

    @Test
    fun `Lambda must capture environment values`() {
        val l = evalWithOutputs("""
            var a = 1
            let b = () : Last -> {a}
            test(b())
            a = 2
            test(b())
        """.trimIndent())

        assertEquals(listOf("1", "1"), l)
    }
}