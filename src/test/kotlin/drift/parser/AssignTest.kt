package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AssignTest {

    private fun parse(code: String): List<DrValue> {
        val outputs = mutableListOf<DrValue>()
        val ast: List<DrStmt> = Parser(lex(code)).parse()
        val env = DrEnv().apply {
            define(
                "print", DrNativeFunction(
                    impl = { _, args ->
                        outputs.add(args[0].second)
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        for (statement in ast) {
            statement.eval(env)
        }

        return outputs
    }

    @Test
    fun `Reassign to immutable variable`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                let a = 1
                a = 2
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign to mutable variable`() {
        assertDoesNotThrow {
            val output = parse("""
                var a = 1
                a = 2
                
                print(a)
            """.trimIndent())

            assertEquals(listOf(DrInt(2)), output.map {
                if (it is DrVariable) it.value
                else it
            })
        }
    }

    @Test
    fun `Reassign to mutable variable with wrong type`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                var a : Int = 1
                a = "Hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign to untyped mutable variable with different type`() {
        assertDoesNotThrow {
            parse("""
                var a = 1
                a = "Hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable using Any type explicitly`() {
        assertDoesNotThrow {
            parse("""
                var x: Any = 1
                x = "hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable with optional class type`() {
        assertDoesNotThrow {
            parse("""
                class User(name: String)
                var u: User? = null
                u = User("John")
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable with wrong class type`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                class User(name: String)
                class Product(id: Int)
                var u: User? = null
                u = Product(1)
            """.trimIndent())
        }
    }

    @Test
    fun `Assign variable in a sub-scope`() {
        assertDoesNotThrow {
            parse("""
                var a: Int
                
                if (true) {
                    a = 42
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Undeclared variable must throw`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                b = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Assign mutable variable with void value must throw`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                fun test {}
                
                var a = test()
            """.trimIndent())
        }
    }
}