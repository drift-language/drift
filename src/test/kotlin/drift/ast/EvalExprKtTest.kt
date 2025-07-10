package drift.ast

import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrNull
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EvalExprKtTest {

    @Test
    fun `Test binary addition`() {
        val expr = Binary(
            Literal(DrInt(2)),
            "+",
            Literal(DrInt(3)))

        val result = expr.eval(DrEnv())

        assertEquals(DrInt(5), result)
    }

    @Test
    fun `Test string concat`() {
        val expr = Binary(
            Literal(DrString("ab")),
            "+",
            Literal(DrString("c")))

        val result = expr.eval(DrEnv())

        assertEquals(DrString("abc"), result)
    }

    @Test
    fun `Test ternary with true condition and else branch`() {
        val expression = Conditional(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(Literal(DrInt(1))),
            elseBranch = ExprStmt(Literal(DrInt(2)))
        )

        val result = expression.eval(DrEnv())

        assertEquals(DrInt(1), result)
    }

    @Test
    fun `Test ternary with false condition and else branch`() {
        val expression = Conditional(
            condition = Literal(DrBool(false)),
            thenBranch = ExprStmt(Literal(DrInt(1))),
            elseBranch = ExprStmt(Literal(DrInt(2)))
        )

        val result = expression.eval(DrEnv())

        assertEquals(DrInt(2), result)
    }

    @Test
    fun `Test ternary with true condition without else branch`() {
        val expression = Conditional(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(Literal(DrInt(1))),
            elseBranch = null
        )

        val result = expression.eval(DrEnv())

        assertEquals(DrInt(1), result)
    }

    @Test
    fun `Test ternary with false condition without else branch`() {
        val expression = Conditional(
            condition = Literal(DrBool(false)),
            thenBranch = ExprStmt(Literal(DrInt(1))),
            elseBranch = null
        )

        val result = expression.eval(DrEnv())

        assertEquals(DrNull, result)
    }
}