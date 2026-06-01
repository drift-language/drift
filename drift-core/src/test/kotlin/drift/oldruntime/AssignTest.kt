package drift.oldruntime

import drift.oldruntime.exceptions.DRCannotAssignToImmutableException
import drift.oldruntime.exceptions.DRCannotUseVoidAsValueException
import drift.oldruntime.exceptions.DRUnassignableException
import drift.oldruntime.exceptions.DRVariableNotDefinedException
import drift.utils.evalProgram
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AssignTest {

    @Test
    fun `Reassign to immutable variable`() {
        assertThrows<DRCannotAssignToImmutableException> {
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
        assertThrows<DRUnassignableException> {
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
                u = User(name = "John")
            """.trimIndent())
        }
    }

    @Test
    fun `Reassign mutable variable with wrong class type`() {
        assertThrows<DRUnassignableException> {
            evalWithOutputs("""
                class User(name: String)
                class Product(id: Int)
                var u: User? = null
                u = Product(id = 1)
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
        assertThrows<DRVariableNotDefinedException> {
            evalWithOutputs("""
                b = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Assign mutable variable with void value must throw`() {
        assertThrows<DRCannotUseVoidAsValueException> {
            evalWithOutputs("""
                fun foo {}
                
                var a = foo()
            """.trimIndent())
        }
    }

    @Test
    fun `Use assignment as value must throw (Void)`() {
        assertThrows<DRCannotUseVoidAsValueException> {
            evalProgram("""
                var b = 0
                let a = b = 1
            """.trimIndent())
        }
    }
}