package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.specials.DrNull
import drift.runtime.values.variables.DrVariable
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AssignTest {

    @Test
    fun `Reassign to immutable variable`() {
        assertThrows<DriftRuntimeException> {
            evalWithOutputs("""
                let a = 1
                a = 2
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign to mutable variable`() {
        assertDoesNotThrow {
            val output = evalWithOutputs("""
                var a = 1
                a = 2
                
                test(a)
            """.trimIndent())

            assertEquals(listOf("2"), output)
        }
    }

    @Test
    fun `Reassign to mutable variable with wrong type`() {
        assertThrows<DriftRuntimeException> {
            evalWithOutputs("""
                var a : Int = 1
                a = "Hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign to untyped mutable variable with different type`() {
        assertDoesNotThrow {
            evalWithOutputs("""
                var a = 1
                a = "Hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable using Any type explicitly`() {
        assertDoesNotThrow {
            evalWithOutputs("""
                var x: Any = 1
                x = "hello"
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable with optional class type`() {
        assertDoesNotThrow {
            evalWithOutputs("""
                class User(name: String)
                var u: User? = null
                u = User("John")
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable with wrong class type`() {
        assertThrows<DriftRuntimeException> {
            evalWithOutputs("""
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
            evalWithOutputs("""
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
            evalWithOutputs("""
                b = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Assign mutable variable with void value must throw`() {
        assertThrows<DriftRuntimeException> {
            evalWithOutputs("""
                fun test {}
                
                var a = test()
            """.trimIndent())
        }
    }
}