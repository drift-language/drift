package drift.ast

import drift.ast.expressions.Binary
import drift.ast.expressions.Conditional
import drift.ast.expressions.Literal
import drift.ast.statements.ExprStmt
import drift.runtime.*
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNull
import drift.runtime.evaluators.eval
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EvalExprKtTest {

    @Test
    fun `Test binary addition`() {
        val expr = Binary(
            Literal(ParserInt(2)),
            "+",
            Literal(ParserInt(3))
        )

        val result = expr.eval(DrEnv())

        assertEquals(ParserInt(5), result)
    }

    @Test
    fun `Test string concat`() {
        val expr = Binary(
            Literal(ParserString("ab")),
            "+",
            Literal(ParserString("c"))
        )

        val result = expr.eval(DrEnv())

        assertEquals(ParserString("abc"), result)
    }

    @Test
    fun `Test ternary with true condition and else branch`() {
        val expression = Conditional(
            condition = Literal(ParserBool(true)),
            thenBranch = ExprStmt(Literal(ParserInt(1))),
            elseBranch = ExprStmt(Literal(ParserInt(2)))
        )

        val result = expression.eval(DrEnv())

        assertEquals(ParserInt(1), result)
    }

    @Test
    fun `Test ternary with false condition and else branch`() {
        val expression = Conditional(
            condition = Literal(ParserBool(false)),
            thenBranch = ExprStmt(Literal(ParserInt(1))),
            elseBranch = ExprStmt(Literal(ParserInt(2)))
        )

        val result = expression.eval(DrEnv())

        assertEquals(ParserInt(2), result)
    }

    @Test
    fun `Test ternary with true condition without else branch`() {
        val expression = Conditional(
            condition = Literal(ParserBool(true)),
            thenBranch = ExprStmt(Literal(ParserInt(1))),
            elseBranch = null
        )

        val result = expression.eval(DrEnv())

        assertEquals(ParserInt(1), result)
    }

    @Test
    fun `Test ternary with false condition without else branch`() {
        val expression = Conditional(
            condition = Literal(ParserBool(false)),
            thenBranch = ExprStmt(Literal(ParserInt(1))),
            elseBranch = null
        )

        val result = expression.eval(DrEnv())

        assertEquals(ParserNull, result)
    }
}