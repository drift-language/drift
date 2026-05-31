package drift.parser

import drift.ast.statements.Import
import drift.lexer.lex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ImportParserTest {

    private fun parse(code: String) = Parser(lex(code)).parse().first() as Import


    @Nested
    inner class NamespaceTests {

        @Test
        fun `simple namespace`() {
            val import = parse("import foo")
            assertEquals("foo", import.namespace)
            assertEquals(listOf("foo"), import.steps)
        }

        @Test
        fun `dotted namespace`() {
            val import = parse("import foo.bar.baz")
            assertEquals("foo.bar.baz", import.namespace)
            assertEquals(listOf("foo", "bar", "baz"), import.steps)
        }
    }


    @Nested
    inner class AliasTests {

        @Test
        fun `import without alias`() {
            val import = parse("import foo")
            assertNull(import.alias)
        }

        @Test
        fun `import with alias`() {
            val import = parse("import foo as f")
            assertEquals("f", import.alias)
        }
    }


    @Nested
    inner class PartialImportTests {

        @Test
        fun `import with single part`() {
            val import = parse("import foo { bar }")
            assertNotNull(import.parts)
            assertEquals(1, import.parts!!.size)
            assertEquals("bar", import.parts!![0].source)
        }

        @Test
        fun `import with multiple parts`() {
            val import = parse("import foo { bar, baz }")
            assertEquals(2, import.parts!!.size)
        }

        @Test
        fun `import part with alias`() {
            val import = parse("import foo { bar as b }")
            assertEquals("b", import.parts!![0].alias)
        }

        @Test
        fun `import without parts has null parts`() {
            val import = parse("import foo")
            assertNull(import.parts)
        }
    }


    @Nested
    inner class WildcardTests {

        @Test
        fun `wildcard import`() {
            val import = parse("import foo { * }")
            assertTrue(import.wildcard)
        }

        @Test
        fun `non-wildcard import`() {
            val import = parse("import foo { bar }")
            assertFalse(import.wildcard)
        }
    }
}
