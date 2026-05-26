package drift.parser

import drift.ast.expressions.Conditional
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConditionalParserTest {

    private fun parseConditional(code: String) =
        (Parser(lex(code)).parse().first() as ExprStmt).expr as Conditional


    @Nested
    inner class ConditionTests {

        @Test
        fun `condition is captured`() {
            val expr = parseConditional("x > 0 ? a : b")
            assertNotNull(expr.condition)
        }
    }


    @Nested
    inner class BranchTests {

        @Test
        fun `then branch is captured`() {
            val expr = parseConditional("x > 0 ? a : b")
            assertNotNull(expr.thenBranch)
        }

        @Test
        fun `else branch is captured when present`() {
            val expr = parseConditional("x > 0 ? a : b")
            assertNotNull(expr.elseBranch)
        }

        @Test
        fun `else branch is null when absent`() {
            val expr = parseConditional("x > 0 ? a")
            assertNull(expr.elseBranch)
        }

        @Test
        fun `then branch with block`() {
            val expr = parseConditional("x > 0 ? { return 1 } : { return 0 }")
            assertNotNull(expr.thenBranch)
            assertNotNull(expr.elseBranch)
        }
    }


    @Nested
    inner class ChainedTests {

        @Test
        fun `chained conditionals parse correctly`() {
            val expr = parseConditional("x == 1 ? a : x == 2 ? b : c")
            assertNotNull(expr.elseBranch)
        }
    }
}
