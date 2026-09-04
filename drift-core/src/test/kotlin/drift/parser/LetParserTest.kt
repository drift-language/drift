package drift.parser

import drift.ast.expressions.Literal
import drift.ast.statements.Let
import drift.lexer.lex
import drift.parser.exceptions.DPUnallowedVariableInjectionPrefixUsageException
import drift.types.*
import drift.values.*
import drift.values.primaries.NumericValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LetParserTest {

    private fun parse(code: String) = Parser(lex(code)).parse().first() as Let


    @Nested
    inner class MutabilityTests {

        @Test
        fun `let produces immutable binding`() {
            val let = parse("let x = 1")
            assertFalse(let.isMutable)
        }

        @Test
        fun `var produces mutable binding`() {
            val let = parse("var x = 1")
            assertTrue(let.isMutable)
        }
    }


    @Nested
    inner class NameTests {

        @Test
        fun `name is correctly captured`() {
            assertEquals("myVar", parse("let myVar = 1").name)
        }

        @Test
        fun `dollar prefix is rejected`() {
            assertThrows<DPUnallowedVariableInjectionPrefixUsageException> {
                parse("let \$x = 1")
            }
        }
    }


    @Nested
    inner class TypeAnnotationTests {

        @Test
        fun `no type annotation defaults to AnyType`() {
            assertEquals(AnyType, parse("let x = 1").type)
        }

        @Test
        fun `explicit ObjectType annotation`() {
            assertEquals(UnresolvedObjectType("Int"), parse("let x: Int = 1").type)
        }

        @Test
        fun `optional type annotation`() {
            assertEquals(UnresolvedOptional(UnresolvedObjectType("Int")), parse("let x: Int? = 1").type)
        }

        @Test
        fun `union type annotation`() {
            val type = parse("let x: Int|String = 1").type as UnresolvedUnion
            assertEquals(listOf(UnresolvedObjectType("Int"), UnresolvedObjectType("String")), type.options)
        }
    }


    @Nested
    inner class ValueTests {

        @Test
        fun `literal integer value`() {
            val value = parse("let x = 1").value as Literal
            assertEquals(NumericValue(1L), value.value)
        }

        @Test
        fun `unassigned let has NotAssignedValue value`() {
            val value = parse("let x: Int").value
            assertNull(value)
        }
    }
}