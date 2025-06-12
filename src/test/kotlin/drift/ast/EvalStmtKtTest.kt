package drift.ast

import drift.runtime.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EvalStmtKtTest {

    @Test
    fun `If statement without else, with TRUE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrFunction { args ->
            output.add(args.joinToString(" ") { it.asString() })
            DrNull
        })

        val stmt = If(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Literal(DrString("yes"))))
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
        env.define("print", DrFunction { args ->
            output.add(args.joinToString(" ") { it.asString() })
            DrNull
        })

        val stmt = If(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Literal(DrString("yes"))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Literal(DrString("no"))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("yes"), output)
    }

    @Test
    fun `If statement with else, with FALSE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrFunction { args ->
            output.add(args.joinToString(" ") { it.asString() })
            DrNull
        })

        val stmt = If(
            condition = Literal(DrBool(false)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Literal(DrString("yes"))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Literal(DrString("no"))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("no"), output)
    }


    @Test
    fun `Block executes all statements in order`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrFunction { args ->
            output.add(args.joinToString(" ") { it.asString() })
            DrNull
        })

        val block = Block(listOf(
            ExprStmt(Call(Variable("print"), listOf(Literal(DrString("yes"))))),
            ExprStmt(Call(Variable("print"), listOf(Literal(DrString("no")))))
        ))

        block.eval(env)

        assertEquals(listOf("yes", "no"), output)
    }

    @Test
    fun `Binary addition of integers`() {
        val expr = Binary(
            left = Literal(DrInt(1)),
            operator = "+",
            right = Literal(DrInt(2))
        )

        val result = expr.eval(DrEnv())

        assertEquals(DrInt(3), result)
    }

    @Test
    fun `Binary addition of strings`() {
        val expr = Binary(
            left = Literal(DrString("ab")),
            operator = "+",
            right = Literal(DrString("c"))
        )

        val result = expr.eval(DrEnv())

        assertEquals(DrString("abc"), result)
    }

    @Test
    fun `Function call executes correctly`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrFunction { args ->
            output.add(args.joinToString(" ") { it.asString() })
            DrNull
        })

        val call = Call(
            callee = Variable("print"),
            args = listOf(Literal(DrString("hello")))
        )

        val result = call.eval(env)

        assertEquals(DrNull, result)
        assertEquals(listOf("hello"), output)
    }
}