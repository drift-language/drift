package drift.ast

import drift.runtime.*
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrNull
import drift.runtime.evaluators.eval
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EvalStmtKtTest {

    @Test
    fun `If statement without else, with TRUE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val stmt = If(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(DrString("yes")))))
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
        env.define("print", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        )
        )

        val stmt = If(
            condition = Literal(DrBool(true)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(DrString("yes")))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(DrString("no")))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("yes"), output)
    }

    @Test
    fun `If statement with else, with FALSE condition`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val stmt = If(
            condition = Literal(DrBool(false)),
            thenBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(DrString("yes")))))
            ),
            elseBranch = ExprStmt(
                Call(Variable("print"), listOf(Argument(null, Literal(DrString("no")))))
            )
        )

        stmt.eval(env)

        assertEquals(listOf("no"), output)
    }


    @Test
    fun `Block executes all statements in order`() {
        val output = mutableListOf<String>()

        val env = DrEnv()
        env.define("print", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val block = Block(listOf(
            ExprStmt(Call(Variable("print"), listOf(Argument(null, Literal(DrString("yes")))))),
            ExprStmt(Call(Variable("print"), listOf(Argument(null, Literal(DrString("no"))))))
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
        env.define("print", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        val call = Call(
            callee = Variable("print"),
            args = listOf(Argument(null, Literal(DrString("hello"))))
        )

        val result = call.eval(env)

        assertEquals(DrNull, result)
        assertEquals(listOf("hello"), output)
    }
}