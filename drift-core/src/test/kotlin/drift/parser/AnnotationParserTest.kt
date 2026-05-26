package drift.parser

import drift.ast.statements.Func
import drift.ast.statements.Let
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnnotationParserTest {

    private fun parseLet(code: String) = Parser(lex(code)).parse().first() as Let
    private fun parseFunc(code: String) = Parser(lex(code)).parse().first() as Func


    @Nested
    inner class NameTests {

        @Test
        fun `annotation name is captured`() {
            val let = parseLet("@Foo\nlet x = 1")
            assertEquals("Foo", let.annotations[0].name)
        }
    }


    @Nested
    inner class TargetTests {

        @Test
        fun `annotation on let`() {
            val let = parseLet("@Foo\nlet x = 1")
            assertEquals(1, let.annotations.size)
        }

        @Test
        fun `annotation on var`() {
            val let = parseLet("@Foo\nvar x = 1")
            assertEquals(1, let.annotations.size)
        }

        @Test
        fun `annotation on function`() {
            val func = parseFunc("@Foo\nfun f() {}")
            assertEquals(1, func.annotations.size)
        }
    }


    @Nested
    inner class ArgumentTests {

        @Test
        fun `annotation without arguments has empty args list`() {
            val let = parseLet("@Foo\nlet x = 1")
            assertTrue(let.annotations[0].args.isEmpty())
        }

        @Test
        fun `annotation with integer argument`() {
            val let = parseLet("@Since(2026)\nlet x = 1")
            assertEquals(1, let.annotations[0].args.size)
        }

        @Test
        fun `annotation with string argument`() {
            val let = parseLet("@Deprecated(\"use other\")\nlet x = 1")
            assertEquals(1, let.annotations[0].args.size)
        }

        @Test
        fun `annotation with multiple arguments`() {
            val let = parseLet("@Meta(1, \"a\")\nlet x = 1")
            assertEquals(2, let.annotations[0].args.size)
        }
    }


    @Nested
    inner class MultipleAnnotationsTests {

        @Test
        fun `multiple annotations on let`() {
            val let = parseLet("@Foo\n@Bar\nlet x = 1")
            assertEquals(2, let.annotations.size)
        }

        @Test
        fun `multiple annotations are ordered`() {
            val let = parseLet("@Foo\n@Bar\nlet x = 1")
            assertEquals("Foo", let.annotations[0].name)
            assertEquals("Bar", let.annotations[1].name)
        }
    }
}
