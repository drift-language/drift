package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AssignTest {

    private fun parse(code: String): List<DrValue> {
        val statements: List<DrStmt> = Parser(lex(code)).parse()
        val outputs = mutableListOf<DrValue>()
        val env = DrEnv().apply {
            define(
                "print", DrNativeFunction(
                    impl = { args ->
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
}