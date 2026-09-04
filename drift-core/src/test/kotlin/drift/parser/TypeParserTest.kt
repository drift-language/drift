package drift.parser

import drift.lexer.lex
import drift.parser.exceptions.DPSpecialInUnionTypeException
import drift.parser.exceptions.DPWrongOptionalUnionTypeException
import drift.types.*
import drift.values.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypeParserTest {

    private fun parseType(code: String): UnresolvedType {
        val let = Parser(lex("let x: $code")).parse().first() as drift.ast.statements.Let
        return let.type
    }


    @Nested
    inner class SimpleTypeTests {

        @Test
        fun `ObjectType is parsed`() {
            assertEquals(UnresolvedObjectType("Int"), parseType("Int"))
        }

        @Test
        fun `AnyType keyword`() {
            assertEquals(AnyType, parseType("Any"))
        }

        @Test
        fun `NullType keyword`() {
            assertEquals(NullType, parseType("Null"))
        }

        @Test
        fun `VoidType keyword`() {
            assertEquals(VoidType, parseType("Void"))
        }
    }


    @Nested
    inner class OptionalTypeTests {

        @Test
        fun `optional type with question mark`() {
            assertEquals(UnresolvedOptional(UnresolvedObjectType("String")), parseType("String?"))
        }
    }


    @Nested
    inner class UnionTypeTests {

        @Test
        fun `union of two types`() {
            val type = parseType("Int|String") as UnresolvedUnion
            assertEquals(listOf(UnresolvedObjectType("Int"), UnresolvedObjectType("String")), type.options)
        }

        @Test
        fun `union of three types`() {
            val type = parseType("Int|String|Bool") as UnresolvedUnion
            assertEquals(2, type.options.size)
            assertTrue(type.options[1] is UnresolvedUnion)
            assertEquals(2, (type.options[1] as UnresolvedUnion).options.size)
        }

        @Test
        fun `optional and union together throws`() {
            assertThrows<DPWrongOptionalUnionTypeException> {
                parseType("Int?|String")
            }
        }

        @Test
        fun `Last in union throws`() {
            assertThrows<DPSpecialInUnionTypeException> {
                parseType("Int|Last")
            }
        }

        @Test
        fun `Any in union throws`() {
            assertThrows<DPSpecialInUnionTypeException> {
                parseType("Int|Any")
            }
        }

        @Test
        fun `Void in union throws`() {
            assertThrows<DPSpecialInUnionTypeException> {
                parseType("Int|Void")
            }
        }
    }
}
