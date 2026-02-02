package drift.parser

import drift.ast.statements.ParserStatement
import drift.runtime.evaluators.eval
import drift.lexer.lex
import drift.parser.exceptions.DPExpectedNewlineBetweenTopLevelStatementsException
import drift.parser.exceptions.DPMissingExpectedTokenException
import drift.runtime.*
import drift.runtime.values.callables.ParserNativeFunction
import drift.runtime.values.specials.ParserNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ParserTest {

    private fun parse(code: String): ParserValue {
        val statements: List<ParserStatement> = Parser(lex(code)).parse()
        val env = DrEnv().apply {
            define(
                "test", ParserNativeFunction(
                    impl = { _, args ->
                        ParserNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }
        var output: ParserValue = ParserNull

        for (statement in statements) {
            output = statement.eval(env)
        }

        return output
    }

    @Test
    fun `Two top-level statements without newline`() {
        assertThrows<DPExpectedNewlineBetweenTopLevelStatementsException> {
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
        assertThrows<DPMissingExpectedTokenException> {
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