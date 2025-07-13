package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.exceptions.DriftParserException
import drift.runtime.*
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.specials.DrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ParserTest {

    private fun parse(code: String): DrValue {
        val statements: List<DrStmt> = Parser(lex(code)).parse()
        val env = DrEnv().apply {
            define(
                "test", DrNativeFunction(
                    impl = { _, args ->
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }
        var output: DrValue = DrNull

        for (statement in statements) {
            output = statement.eval(env)
        }

        return output
    }

    @Test
    fun `Two top-level statements without newline`() {
        assertThrows<DriftParserException> {
            parse("""
                test("hello") test("world")
            """.trimIndent())
        }
    }

    @Test
    fun `Two top-level statements with newline`() {
        assertDoesNotThrow {
            parse("""
                test("hello")
                test("world")
            """.trimIndent())
        }
    }

    @Test
    fun `Two block statements without newline`() {
        assertThrows<DriftParserException> {
            parse("""
                {
                    test("hello") test("world")
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Two block statements with newline`() {
        assertDoesNotThrow {
            parse("""
                {
                    test("hello")
                    test("world")
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Statement with newline in it`() {
        assertDoesNotThrow {
            parse("""
                test(
                    1
                )
            """.trimIndent())
        }
    }
}