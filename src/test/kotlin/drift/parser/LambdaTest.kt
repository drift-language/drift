package drift.parser

import drift.ast.Function
import drift.ast.Lambda
import drift.ast.eval
import drift.exceptions.DriftParserException
import drift.runtime.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LambdaTest {

    private fun lambda(l: String) : MutableList<String> {
        val output = mutableListOf<String>()

        val env = DrEnv().apply {
            define(
                "print", DrNativeFunction(
                    impl = { args ->
                        args.map { output.add(it.second.asString()) }
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }

        val tokens = lex(l)
        val statements = Parser(tokens).parse()

        for (statement in statements) {
            statement.eval(env)
        }

        return output
    }

    @Test
    fun `Lambda without parameter`() {
        val l = lambda("""
            print(() -> { return 42 } ())
        """.trimIndent())

        assertEquals(listOf("42"), l)
    }

    @Test
    fun `Lambda with one implicitly typed parameter`() {
        val l = lambda("""
            print((x) -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter`() {
        val l = lambda("""
            print((x: Int) -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with two implicitly typed parameters`() {
        val l = lambda("""
            print((x, y) -> { return x + y } (1, 2))
        """.trimIndent())

        assertEquals(listOf("3"), l)
    }

    @Test
    fun `Lambda with two explicitly typed parameters`() {
        val l = lambda("""
            print((x: Int, y: Int) -> { return x + y } (1, 2))
        """.trimIndent())

        assertEquals(listOf("3"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter and union return type`() {
        val l = lambda("""
            print((x: Int): Int|String -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly union typed parameter and return type`() {
        val l = lambda("""
            print((x: Int|String): Int -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly optional typed parameter and return type`() {
        val l = lambda("""
            print((x: Int?): Int -> { return x } (1))
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with one explicitly typed parameter and optional return type`() {
        val l = lambda("""
            print((x: Int): Int? -> { return null } (1))
        """.trimIndent())

        assertEquals(listOf("null"), l)
    }

    @Test
    fun `Lambda with Last special return type`() {
        val l = lambda("""
            print((): Last -> { 1 } ())
        """.trimIndent())

        assertEquals(listOf("1"), l)
    }

    @Test
    fun `Lambda with same parameter defined two times must throw exception`() {
        assertThrows<DriftParserException> {
            lambda("""
                print((x, x) -> { return x } ())
            """.trimIndent())
        }
    }
}