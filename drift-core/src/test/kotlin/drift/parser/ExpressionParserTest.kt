package drift.parser

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.ExprStmt
import drift.lexer.lex
import drift.parser.exceptions.DPInvalidAssignmentTargetException
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.primaries.ParserString
import drift.oldruntime.values.primaries.ParserNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExpressionParserTest {

    private fun parseExpr(code: String): ParserExpression =
        (Parser(lex(code)).parse().first() as ExprStmt).expr


    @Nested
    inner class LiteralTests {

        @Test
        fun `integer literal`() {
            val expr = parseExpr("1") as Literal
            assertEquals(ParserInt(1), expr.value)
        }

        @Test
        fun `string literal`() {
            val expr = parseExpr("\"hello\"") as Literal
            assertEquals(ParserString("hello"), expr.value)
        }

        @Test
        fun `null literal`() {
            val expr = parseExpr("null") as Literal
            assertEquals(ParserNull, expr.value)
        }
    }


    @Nested
    inner class UnaryTests {

        @Test
        fun `negation operator`() {
            val expr = parseExpr("-1") as Unary
            assertEquals("-", expr.operator)
        }

        @Test
        fun `logical not operator`() {
            val expr = parseExpr("!true") as Unary
            assertEquals("!", expr.operator)
        }
    }


    @Nested
    inner class BinaryTests {

        @Test
        fun `addition`() {
            val expr = parseExpr("1 + 2") as Binary
            assertEquals("+", expr.operator)
        }

        @Test
        fun `subtraction`() {
            val expr = parseExpr("3 - 1") as Binary
            assertEquals("-", expr.operator)
        }

        @Test
        fun `multiplication`() {
            val expr = parseExpr("2 * 3") as Binary
            assertEquals("*", expr.operator)
        }

        @Test
        fun `division`() {
            val expr = parseExpr("6 / 2") as Binary
            assertEquals("/", expr.operator)
        }

        @Test
        fun `equality`() {
            val expr = parseExpr("a == b") as Binary
            assertEquals("==", expr.operator)
        }

        @Test
        fun `inequality`() {
            val expr = parseExpr("a != b") as Binary
            assertEquals("!=", expr.operator)
        }

        @Test
        fun `less than`() {
            val expr = parseExpr("a < b") as Binary
            assertEquals("<", expr.operator)
        }

        @Test
        fun `greater than`() {
            val expr = parseExpr("a > b") as Binary
            assertEquals(">", expr.operator)
        }

        @Test
        fun `operator precedence multiplication before addition`() {
            val expr = parseExpr("1 + 2 * 3") as Binary
            assertEquals("+", expr.operator)
            assertEquals("*", (expr.right as Binary).operator)
        }
    }


    @Nested
    inner class VariableTests {

        @Test
        fun `variable access`() {
            val expr = parseExpr("x") as Variable
            assertEquals("x", expr.name)
        }
    }


    @Nested
    inner class AssignTests {

        @Test
        fun `variable assignment`() {
            val expr = parseExpr("x = 1") as Assign
            assertEquals("x", expr.name)
        }

        @Test
        fun `assignment to non-target throws`() {
            assertThrows<DPInvalidAssignmentTargetException> {
                parseExpr("1 = 2")
            }
        }
    }


    @Nested
    inner class GetSetTests {

        @Test
        fun `member access`() {
            val expr = parseExpr("obj.field") as Get
            assertEquals("field", expr.name)
            assertEquals("obj", (expr.receiver as Variable).name)
        }

        @Test
        fun `member assignment`() {
            val expr = parseExpr("obj.field = 1") as Set
            assertEquals("field", expr.name)
            assertEquals("obj", (expr.receiver as Variable).name)
        }
    }


    @Nested
    inner class CallTests {

        @Test
        fun `function call without arguments`() {
            val expr = parseExpr("foo()") as Call
            assertTrue(expr.args.isEmpty())
        }

        @Test
        fun `function call with positional argument`() {
            val expr = parseExpr("foo(1)") as Call
            assertEquals(1, expr.args.size)
            assertNull(expr.args[0].name)
        }

        @Test
        fun `function call with named argument`() {
            val expr = parseExpr("foo(x = 1)") as Call
            assertEquals("x", expr.args[0].name)
        }

        @Test
        fun `function call with multiple arguments`() {
            val expr = parseExpr("foo(1, 2)") as Call
            assertEquals(2, expr.args.size)
        }
    }


    @Nested
    inner class ConditionalTests {

        @Test
        fun `ternary expression`() {
            val expr = parseExpr("a ? 1 : 2") as Conditional
            assertEquals("a", (expr.condition as Variable).name)
        }

        @Test
        fun `ternary without else branch`() {
            val expr = parseExpr("a ? 1") as Conditional
            assertNull(expr.elseBranch)
        }
    }
}
