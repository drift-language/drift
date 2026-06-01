package drift.parser

import drift.ast.expressions.Lambda
import drift.ast.statements.ExprStmt
import drift.ast.statements.Let
import drift.lexer.lex
import drift.parser.exceptions.DPParameterAlreadyDefinedException
import drift.oldruntime.AnyType
import drift.oldruntime.ObjectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LambdaParserTest {

    private fun parseLambdaFromLet(code: String): Lambda {
        val let = Parser(lex(code)).parse().first() as Let
        return let.value as Lambda
    }

    private fun parseLambdaExpr(code: String): Lambda =
        (Parser(lex(code)).parse().first() as ExprStmt).expr as Lambda


    @Nested
    inner class ParameterTests {

        @Test
        fun `lambda with no parameters`() {
            val lambda = parseLambdaFromLet("let f = () -> {}")
            assertTrue(lambda.parameters.isEmpty())
        }

        @Test
        fun `lambda with single parameter`() {
            val lambda = parseLambdaFromLet("let f = (x) -> {}")
            assertEquals(1, lambda.parameters.size)
            assertEquals("x", lambda.parameters[0].name)
        }

        @Test
        fun `lambda parameter with type`() {
            val lambda = parseLambdaFromLet("let f = (x: Int) -> {}")
            assertEquals(ObjectType("Int"), lambda.parameters[0].type)
        }

        @Test
        fun `lambda parameter without type defaults to AnyType`() {
            val lambda = parseLambdaFromLet("let f = (x) -> {}")
            assertEquals(AnyType, lambda.parameters[0].type)
        }

        @Test
        fun `lambda with multiple parameters`() {
            val lambda = parseLambdaFromLet("let f = (x: Int, y: String) -> {}")
            assertEquals(2, lambda.parameters.size)
        }

        @Test
        fun `duplicate parameter name throws`() {
            assertThrows<DPParameterAlreadyDefinedException> {
                parseLambdaFromLet("let f = (x, x) -> {}")
            }
        }
    }


    @Nested
    inner class ReturnTypeTests {

        @Test
        fun `lambda without return type defaults to AnyType`() {
            val lambda = parseLambdaFromLet("let f = () -> {}")
            assertEquals(AnyType, lambda.returnType)
        }

        @Test
        fun `lambda with explicit return type`() {
            val lambda = parseLambdaFromLet("let f = (): Int -> {}")
            assertEquals(ObjectType("Int"), lambda.returnType)
        }
    }
}
