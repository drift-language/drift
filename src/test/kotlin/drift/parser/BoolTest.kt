package drift.parser

import drift.ast.eval
import drift.runtime.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BoolTest {

    private fun evalExpr(input: String) : DrValue {
        val tokens = lex(input)
        val parser = Parser(tokens)
        val expression = parser.parse()
        val env = DrEnv()

        var result: DrValue = DrNull

        expression.forEach {
            result = it.eval(env)
        }

        return result
    }

    @Test
    fun `Compare two same integers with == operator`() {
        val exp = evalExpr("1 == 1")

        assertEquals(exp.type(), BoolType)
        assertEquals(exp, DrBool(true))
    }

    @Test
    fun `Compare two different integers with == operator`() {
        val exp = evalExpr("1 == 2")

        assertEquals(exp.type(), BoolType)
        assertEquals(exp, DrBool(false))
    }

    @Test
    fun `Compare two same integers with != operator`() {
        val exp = evalExpr("1 != 1")

        assertEquals(exp.type(), BoolType)
        assertEquals(exp, DrBool(false))
    }

    @Test
    fun `Compare two different integers with != operator`() {
        val exp = evalExpr("1 != 2")

        assertEquals(exp.type(), BoolType)
        assertEquals(exp, DrBool(true))
    }
}