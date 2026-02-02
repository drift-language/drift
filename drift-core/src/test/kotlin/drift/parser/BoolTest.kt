package drift.parser

import drift.lexer.lex
import drift.runtime.evaluators.eval
import drift.runtime.*
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.specials.ParserNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals

class BoolTest {

    private fun evalExpr(input: String) : ParserValue {
        val tokens = lex(input)
        val parser = Parser(tokens)
        val expression = parser.parse()
        val env = DrEnv()

        var result: ParserValue = ParserNull

        expression.forEach {
            result = it.eval(env)
        }

        return result
    }

    @Test
    fun `Compare two same integers with == operator`() {
        val exp = evalExpr("1 == 1")

        assertEquals(exp.type(), ObjectType("Bool"))
        assertEquals(exp, ParserBool(true))
    }

    @Test
    fun `Compare two different integers with == operator`() {
        val exp = evalExpr("1 == 2")

        assertEquals(exp.type(), ObjectType("Bool"))
        assertEquals(exp, ParserBool(false))
    }

    @Test
    fun `Compare two same integers with != operator`() {
        val exp = evalExpr("1 != 1")

        assertEquals(exp.type(), ObjectType("Bool"))
        assertEquals(exp, ParserBool(false))
    }

    @Test
    fun `Compare two different integers with != operator`() {
        val exp = evalExpr("1 != 2")

        assertEquals(exp.type(), ObjectType("Bool"))
        assertEquals(exp, ParserBool(true))
    }

    @Test
    fun `Compare two booleans with AND operator`() {
        assertDoesNotThrow {
            val exp = evalExpr("true && true")

            assertEquals(exp.type(), ObjectType("Bool"))
            assertEquals(exp, ParserBool(true))
        }
    }

    @Test
    fun `Compare two booleans with OR operator`() {
        assertDoesNotThrow {
            val exp = evalExpr("true || true")

            assertEquals(exp.type(), ObjectType("Bool"))
            assertEquals(exp, ParserBool(true))
        }
    }
}