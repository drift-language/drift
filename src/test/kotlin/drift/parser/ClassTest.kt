package drift.parser

import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.utils.evalProgram
import drift.utils.evalWithOutput
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ClassTest {

    @Test
    fun `Class with correct instance creation`() {
        val result = evalWithOutput("""
            class User(name: String)
            let u1 = User("John")
            test(u1.name)
        """.trimIndent())

        assertEquals(result, "John")
    }

    @Test
    fun `Class with missing constructor argument`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class User(name: String)
                let u = User()
            """.trimIndent())
        }
    }

    @Test
    fun `Class with too many constructor arguments`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class User(name: String)
                let u = User("John", 1)
            """.trimIndent())
        }
    }

    @Test
    fun `Class with property access`() {
        val result = evalWithOutput("""
            class User(name: String)
            let u = User("John")
            test(u.name)
        """.trimIndent())

        assertEquals(result, "John")
    }

    @Test
    fun `Class with unknown property`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class User(name: String)
                let u = User("John")
                print(u.age)
            """.trimIndent())
        }
    }

    @Test
    fun `Class with method call`() {
        val result = evalWithOutput("""
            class User(name: String) {
                fun hello {
                    return "Hello, " + this.name
                }
            }
            
            let u = User("John")
            test(u.hello())
        """.trimIndent())

        assertEquals(result, "Hello, John")
    }

    @Test
    fun `Class with unknown method`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                class User(name: String)
                let u = User("John")
                u.unknown()
            """.trimIndent())
        }
    }

    @Test
    fun `Assign class attribute`() {
        val result = evalWithOutput("""
            class User(name: String)
            let u = User("John")
            u.name = "Bob"
            test(u.name)
        """.trimIndent())

        assertEquals(result, "Bob")
    }

    @Test
    fun `Assign property on non-instance`() {
        assertThrows<DriftRuntimeException> {
            evalProgram("""
                let x = 5
                x.name = "Bob"
            """.trimIndent())
        }
    }

    @Test
    fun `Class with empty block`() {
        val result = evalWithOutput("""
            class User(name: String) {}
            let u = User("John")
            test(u.name)
        """.trimIndent())

        assertEquals("John", result)
    }

    @Test
    fun `Class with no asString method fallback`() {
        val result = evalWithOutput("""
            class User(name: String)
            let u = User("John")
            test(u)
        """.trimIndent())

        assertEquals("<class User | instance", result.substring(0, 22))
    }

    @Test
    fun `Class with invalid asString return type`() {
        assertThrows<DriftTypeException> {
            evalWithOutput("""
                class User(name: String) {
                    fun asString : Int { return 42 }
                }
                let u = User("Jon")
                test(u)
            """.trimIndent())
        }
    }

    @Test
    fun `Call method with no parameters`() {
        val result = evalWithOutput("""
            class User(name: String) {
                fun hello { return "hi " + this.name }
            }
            let u = User("John")
            test(u.hello())
        """.trimIndent())

        assertEquals("hi John", result)
    }

    @Test
    fun `Multiple methods in class`() {
        val result = evalWithOutputs("""
            class User(name: String) {
                fun hello { return "hi " + this.name }
                fun bye { return "bye " + this.name }
            }
            let u = User("John")
            test(u.hello())
            test(u.bye())
        """.trimIndent())

        assertEquals(listOf("hi John", "bye John"), result)
    }

    @Test
    fun `Class with optional field`() {
        val result = evalWithOutput("""
            class User(name: String, age: Int?)
            let u = User("John", null)
            test(u.age)
        """.trimIndent())

        assertEquals(result, "null")
    }
}