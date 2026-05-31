package drift.parser

import drift.ast.statements.Class
import drift.lexer.lex
import drift.parser.exceptions.DPOnlyOneConstructorPerClassException
import drift.parser.exceptions.DPOnlyOneStaticBlockPerClassException
import drift.parser.exceptions.DPUnexpectedStatementInClassBodyException
import drift.runtime.ObjectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ClassParserTest {

    private fun parse(code: String) = Parser(lex(code)).parse().first() as Class


    @Nested
    inner class NameTests {

        @Test
        fun `name is correctly captured`() {
            assertEquals("Foo", parse("class Foo {}").name)
        }
    }


    @Nested
    inner class PrimaryConstructorTests {

        @Test
        fun `class without primary constructor`() {
            assertFalse(parse("class Foo {}").hasPrimaryConstructor)
        }

        @Test
        fun `class with primary constructor`() {
            assertTrue(parse("class Foo(x: Int) {}").hasPrimaryConstructor)
        }

        @Test
        fun `primary constructor parameters become fields`() {
            val clazz = parse("class Foo(x: Int) {}")
            assertEquals(1, clazz.fields.size)
            assertEquals("x", clazz.fields[0].name)
            assertEquals(ObjectType("Int"), clazz.fields[0].type)
        }

        @Test
        fun `primary constructor and explicit init throws`() {
            assertThrows<DPOnlyOneConstructorPerClassException> {
                parse("class Foo(x: Int) {\n    init(y: Int) {}\n}")
            }
        }

        @Test
        fun `two explicit init blocks throw`() {
            assertThrows<DPOnlyOneConstructorPerClassException> {
                parse("class Foo {\n    init(x: Int) {}\n    init(y: Int) {}\n}")
            }
        }
    }


    @Nested
    inner class FieldTests {

        @Test
        fun `field is registered`() {
            val clazz = parse("class Foo {\n    let x = 1\n}")
            assertEquals(1, clazz.fields.size)
            assertEquals("x", clazz.fields[0].name)
        }

        @Test
        fun `mutable field is registered`() {
            val clazz = parse("class Foo {\n    var x = 1\n}")
            assertTrue(clazz.fields[0].isMutable)
        }
    }


    @Nested
    inner class MethodTests {

        @Test
        fun `method is registered`() {
            val clazz = parse("class Foo {\n    fun greet {}\n}")
            assertEquals(1, clazz.methods.size)
            assertEquals("greet", clazz.methods[0].name)
        }
    }


    @Nested
    inner class StaticBlockTests {

        @Test
        fun `static field is registered`() {
            val clazz = parse("class Foo {\n    static {\n        let count = 0\n    }\n}")
            assertEquals(1, clazz.staticFields.size)
            assertEquals("count", clazz.staticFields[0].name)
        }

        @Test
        fun `static method is registered`() {
            val clazz = parse("class Foo {\n    static {\n        fun create {}\n    }\n}")
            assertEquals(1, clazz.staticMethods.size)
            assertEquals("create", clazz.staticMethods[0].name)
        }

        @Test
        fun `two static blocks throw`() {
            assertThrows<DPOnlyOneStaticBlockPerClassException> {
                parse("class Foo {\n    static {}\n    static {}\n}")
            }
        }
    }


    @Nested
    inner class BodyValidationTests {

        @Test
        fun `unexpected statement in class body throws`() {
            assertThrows<DPUnexpectedStatementInClassBodyException> {
                parse("class Foo {\n    1\n}")
            }
        }

        @Test
        fun `empty class body is valid`() {
            assertDoesNotThrow { parse("class Foo {}") }
        }

        @Test
        fun `class without body is valid`() {
            assertDoesNotThrow { parse("class Foo") }
        }
    }
}