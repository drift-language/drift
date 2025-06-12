package drift.parser

import drift.ast.eval
import drift.runtime.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BinaryExprTest {

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
    fun `Test simple addition`() {
        val result = evalExpr("1 + 2")

        assertEquals(DrInt(3), result)
    }

    @Test
    fun `Test simple concatenation`() {
        val result = evalExpr("\"a\" + \"b\"")

        assertEquals(DrString("ab"), result)
    }

    @Test
    fun `Test mixed precedence 1 ADD 2 MUL 3`() {
        val result = evalExpr("1 + 2 * 3")

        assertEquals(DrInt(7), result)
    }

    @Test
    fun `Test parenthesized expression`() {
        val result = evalExpr("(1 + 2) * 3")

        assertEquals(DrInt(9), result)
    }

    @Test
    fun `Test equality true`() {
        val result = evalExpr("1 + 1 == 2")

        assertEquals(DrBool(true), result)
    }
}