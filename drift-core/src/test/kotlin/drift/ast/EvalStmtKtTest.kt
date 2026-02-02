package drift.ast

import drift.ast.expressions.Argument
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.Literal
import drift.ast.expressions.Variable
import drift.ast.statements.Block
import drift.ast.statements.ExprStmt
import drift.ast.statements.If
import drift.runtime.*
import drift.runtime.values.callables.ParserNativeFunction
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNull
import drift.runtime.evaluators.eval
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EvalStmtKtTest {

    @Test
    fun `If statement without else, with TRUE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val stmt = If(
            condition = Literal(ParserBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(ParserString("yes")))))
            ),
            elseBranch = null
        )

        stmt.eval(env)

        assertEquals(listOf("yes"), output)
    }

    @Test
    fun `If statement with else, with TRUE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        )
        )

        val stmt = If(
            condition = Literal(ParserBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(ParserString("yes")))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(ParserString("no")))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("yes"), output)
    }

    @Test
    fun `If statement with else, with FALSE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val stmt = If(
            condition = Literal(ParserBool(false)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(ParserString("yes")))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(ParserString("no")))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("no"), output)
    }


    @Test
    fun `Block executes all statements in order`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val block = Block(
            listOf(
                ExprStmt(Call(Variable("print"), listOf(Argument(null, Literal(ParserString("yes")))))),
                ExprStmt(Call(Variable("print"), listOf(Argument(null, Literal(ParserString("no"))))))
            )
        )

        block.eval(env)

        assertEquals(listOf("yes", "no"), output)
    }

    @Test
    fun `Binary addition of integers`() {
        val expr = Binary(
            left = Literal(ParserInt(1)),
            operator = "+",
            right = Literal(ParserInt(2))
        )

        val result = expr.eval(DrEnv())

        assertEquals(ParserInt(3), result)
    }

    @Test
    fun `Binary addition of strings`() {
        val expr = Binary(
            left = Literal(ParserString("ab")),
            operator = "+",
            right = Literal(ParserString("c"))
        )

        val result = expr.eval(DrEnv())

        assertEquals(ParserString("abc"), result)
    }

    @Test
    fun `Function call executes correctly`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val call = Call(
            callee = Variable("print"),
            args = listOf(Argument(null, Literal(ParserString("hello"))))
        )

        val result = call.eval(env)

        assertEquals(ParserNull, result)
        assertEquals(listOf("hello"), output)
    }
}