package drift.parser

import drift.checkers.collectors.exceptions.DCAmbiguousMemberNameException
import drift.runtime.exceptions.*
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
            let u1 = User(name = "John")
            test(u1.name)
        """.trimIndent())

        assertEquals(result, "John")
    }

    @Test
    fun `Class with missing constructor argument`() {
        assertThrows<DRWrongNumberOfClassArgumentsException> {
            evalProgram("""
                class User(name: String)
                let u = User()
            """.trimIndent())
        }
    }

    @Test
    fun `Class with too many constructor arguments`() {
        assertThrows<DRWrongNumberOfClassArgumentsException> {
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
            let u = User(name = "John")
            test(u.name)
        """.trimIndent())

        assertEquals(result, "John")
    }

    @Test
    fun `Class with unknown property`() {
        assertThrows<DRUnknownClassMemberException> {
            evalProgram("""
                class User(name: String)
                let u = User(name = "John")
                u.age
            """.trimIndent())
        }
    }

    @Test
    fun `Class with method call`() {
        val result = evalWithOutput("""
            class User(name: String) {
                fun hello {
                    return "Hello, " + ${'$'}this.name
                }
            }
            
            let u = User(name = "John")
            test(u.hello())
        """.trimIndent())

        assertEquals("Hello, John", result)
    }

    @Test
    fun `Class with unknown method`() {
        assertThrows<DRUnknownClassMemberException> {
            evalProgram("""
                class User(name: String)
                let u = User(name = "John")
                u.unknown()
            """.trimIndent())
        }
    }

    @Test
    fun `Assign class attribute from primary constructor must throw`() {
        assertThrows<DRCannotAssignToImmutableException> {
            evalProgram("""
                class User(name: String)
                let u = User(name = "John")
                u.name = "Bob"
                u.name
            """.trimIndent())
        }
    }

    @Test
    fun `Assign property on non-instance`() {
        assertThrows<DRCannotSetObjectException> {
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
            let u = User(name = "John")
            test(u.name)
        """.trimIndent())

        assertEquals("John", result)
    }

    @Test
    fun `Class with no asString method fallback`() {
        val result = evalWithOutput("""
            class User(name: String)
            let u = User(name = "John")
            test(u)
        """.trimIndent())

        assertEquals("<[class#", result.substring(0, 8))
    }

    @Test
    fun `Class with invalid asString return type`() {
        assertThrows<DRUnsuccessfulCastException> {
            evalWithOutput("""
                class User(name: String) {
                    fun asString : Int { return 42 }
                }
                let u = User(name = "Jon")
                test(u)
            """.trimIndent())
        }
    }

    @Test
    fun `Class with valid asString must return custom string`() {
        evalWithOutput("""
            class U {
                fun asString : String {
                    return "Hello"
                }
            }   
            let u = U()
            test(u)
        """.trimIndent())
    }

    @Test
    fun `Call method with no parameters`() {
        val result = evalWithOutput("""
            class User(name: String) {
                fun hello { return "hi " + ${'$'}this.name }
            }
            let u = User(name = "John")
            test(u.hello())
        """.trimIndent())

        assertEquals("hi John", result)
    }

    @Test
    fun `Multiple methods in class`() {
        val result = evalWithOutputs("""
            class User(name: String) {
                fun hello { return "hi " + ${'$'}this.name }
                fun bye { return "bye " + ${'$'}this.name }
            }
            let u = User(name = "John")
            test(u.hello())
            test(u.bye())
        """.trimIndent())

        assertEquals(listOf("hi John", "bye John"), result)
    }

    @Test
    fun `Class with optional field`() {
        val result = evalWithOutput("""
            class User(name: String, age: Int?)
            let u = User(name = "John", age = null)
            test(u.age)
        """.trimIndent())

        assertEquals(result, "null")
    }

    @Test
    fun `Constructor with mixed positional and named args`() {
        assertThrows<DRPositionalArgumentsNotAllowedException> {
            evalProgram("""
                class User(name: String, age: Int)
                let u = User("John", age = 20)
                test(u.age)
            """.trimIndent())
        }
    }

    @Test
    fun `Duplicate named constructor argument must throw`() {
        assertThrows<DRWrongNumberOfClassArgumentsException> {
            evalProgram("""
                class User(name: String)
                let u = User(name = "John", name = "Bob")
            """.trimIndent())
        }
    }

    @Test
    fun `Unassigned field access must throw`() {
        assertThrows<DRCannotUseUnassignedEntityException> {
            evalProgram("""
                class A {
                    let x: Int
                }
                let a = A()
                test(a.x)
            """.trimIndent())
        }
    }

    @Test
    fun `Assign immutable field must throw`() {
        assertThrows<DRCannotAssignToImmutableException> {
            evalProgram("""
                class A {
                    let x = 1
                }
                let a = A()
                a.x = 2
            """.trimIndent())
        }
    }

    @Test
    fun `Assign mutable field must succeed`() {
        val result = evalWithOutput("""
            class A {
                var x = 1
            }
            let a = A()
            a.x = 2
            test(a.x)
        """.trimIndent())

        assertEquals("2", result)
    }

    @Test
    fun `Using this outside method must throw`() {
        assertThrows<DRVariableNotDefinedException> {
            evalProgram("""
                class A {
                    let x = ${'$'}this
                }
                
                A()
            """.trimIndent())
        }
    }

    @Test
    fun `this must reference current instance`() {
        val result = evalWithOutputs("""
            class A(name: String) {
                fun id { return ${'$'}this.name }
            }
            let a = A(name = "x")
            let b = A(name = "y")
            test(a.id())
            test(b.id())
        """.trimIndent())

        assertEquals(listOf("x", "y"), result)
    }

    @Test
    fun `Method can mutate var field`() {
        val result = evalWithOutput("""
            class A {
                var x = 1
                fun inc { ${'$'}this.x = ${'$'}this.x + 1 }
            }
            let a = A()
            a.inc()
            test(a.x)
        """.trimIndent())

        assertEquals("2", result)
    }

    @Test
    fun `Calling instance method on class must throw`() {
        assertThrows<DRUnknownClassMemberException> {
            evalProgram("""
                class A {
                    fun hello { return "hi" }
                }
                A.hello()
            """.trimIndent())
        }
    }

    @Test
    fun `Instances must not share fields`() {
        val result = evalWithOutputs("""
            class A {
                var x = 0
            }
            let a = A()
            let b = A()
            a.x = 5
            test(a.x)
            test(b.x)
        """.trimIndent())

        assertEquals(listOf("5", "0"), result)
    }

    @Test
    fun `Assign static let must throw`() {
        assertThrows<DRCannotAssignToImmutableException> {
            evalProgram("""
                class A {
                    static {
                        let x = 1
                    }
                }
                A.x = 2
            """.trimIndent())
        }
    }

    @Test
    fun `Assign static var must succeed`() {
        val result = evalWithOutput("""
            class A {
                static {
                    var x = 1
                }
            }
            A.x = 2
            test(A.x)
        """.trimIndent())

        assertEquals("2", result)
    }

    @Test
    fun `Field and method name collision must throw`() {
        assertThrows<DCAmbiguousMemberNameException> {
            evalProgram("""
                class A {
                    let x = 1
                    fun x { return 2 }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Primary constructor must reject positional arguments`() {
        assertThrows<DRPositionalArgumentsNotAllowedException> {
            evalProgram("""
            class A(x: Int, y: Int)
            let a = A(1, 2)
        """.trimIndent())
        }
    }

    @Test
    fun `Primary constructor must reject mixed arguments`() {
        assertThrows<DRPositionalArgumentsNotAllowedException> {
            evalProgram("""
            class A(x: Int, y: Int)
            let a = A(y = 1, 2)
        """.trimIndent())
        }
    }

    @Test
    fun `Standard constructor must reject positional after named`() {
        assertThrows<DRPositionalMustPrecedeNamedArgumentsException> {
            evalProgram("""
            class A {
                fun init(x: Int, y: Int) {}
            }
            let a = A(y = 1, 2)
        """.trimIndent())
        }
    }

    @Test
    fun `Positional and named binding to same parameter must throw`() {
        assertThrows<DRArgumentAlreadyBoundException> {
            evalProgram("""
            fun f(a: Int, b: Int) {}
            f(1, a = 2)
        """.trimIndent())
        }
    }

    @Test
    fun `Unknown named constructor argument must throw`() {
        assertThrows<DRUnknownParameterException> {
            evalProgram("""
            class A(x: Int)
            let a = A(y = 2)
        """.trimIndent())
        }
    }

    @Test
    fun `Unassigned field with default value must be initialized`() {
        val result = evalWithOutput("""
        class A {
            let x: Int = 5
        }
        let a = A()
        test(a.x)
    """.trimIndent())

        assertEquals("5", result)
    }

    @Test
    fun `Accessing static field through instance must throw`() {
        assertThrows<DRUnknownClassMemberException> {
            evalProgram("""
            class A {
                static {
                    let x = 1
                }
            }
            let a = A()
            a.x
        """.trimIndent())
        }
    }

    @Test
    fun `Accessing instance field through class must throw`() {
        assertThrows<DRUnknownClassStaticMemberException> {
            evalProgram("""
            class A {
                let x = 1
            }
            A.x
        """.trimIndent())
        }
    }

    @Test
    fun `Empty class without asString must fallback`() {
        val result = evalWithOutput("""
        class A {}
        let a = A()
        test(a)
    """.trimIndent())

        assertEquals("<[class#", result.substring(0, 8))
    }

    @Test
    fun `Calling native method on instance must work`() {
        val result = evalWithOutput("""
        test("hello".length())
    """.trimIndent())

        assertEquals("5", result)
    }

    @Test
    fun `Use dynamic field assignment as value must throw (Void)`() {
        assertThrows<DRUnsupportedOperatorException> {
            evalProgram("""
                class A { var x = 1 }
                let _ = A()
                let y = (_.x = 2) + 1
            """.trimIndent())
        }
    }
}