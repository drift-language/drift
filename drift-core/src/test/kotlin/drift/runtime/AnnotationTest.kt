package drift.runtime

import drift.parser.exceptions.DPUnsupportedAnnotationException
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AnnotationTest {

    // -------------------------------------------------------------------------
    // Supported targets: let / var
    // parseLet consumes storedAnnotations → no exception
    // -------------------------------------------------------------------------

    @Test
    fun `Annotation on let declaration must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                let x = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation on var declaration must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                var x = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with string arg on let must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Deprecated("use other instead")
                let value = 42
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with integer arg on let must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Since(2026)
                let x = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with multiple args on let must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Range(min = 0, max = 100)
                let x = 50
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Supported targets: fun
    // parseFunction consumes storedAnnotations → no exception
    // -------------------------------------------------------------------------

    @Test
    fun `Annotation on function must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                fun test() {}
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with string arg on function must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Deprecated("use other instead")
                fun test() {}
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with multiple args on function must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Tag(name = "x", version = 1)
                fun test() {}
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Supported targets: class
    // parseClass consumes storedAnnotations → no exception
    // -------------------------------------------------------------------------

    @Test
    fun `Annotation on class must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                class Bar {}
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation with arg on class must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Serializable("json")
                class Payload {}
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Supported targets: class members
    // -------------------------------------------------------------------------

    @Test
    fun `Annotation on class method must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                class A {
                    @Override
                    fun hello { return "hi" }
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation on class field must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                class A {
                    @NotNull
                    let x = 1
                }
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Stacked annotations on supported targets
    // Each annotation is added then all are consumed by the target parser
    // -------------------------------------------------------------------------

    @Test
    fun `Multiple annotations stacked on function must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                @Bar
                fun test() {}
            """.trimIndent())
        }
    }

    @Test
    fun `Multiple annotations stacked on let must succeed`() {
        assertDoesNotThrow {
            evalProgram("""
                @Foo
                @Bar
                let x = 1
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Unsupported targets: if / for / expression / return
    // storedAnnotations is not consumed → DPUnsupportedAnnotationException
    // -------------------------------------------------------------------------

    @Test
    fun `Annotation on if statement must throw unsupported`() {
        assertThrows<DPUnsupportedAnnotationException> {
            evalProgram("""
                @Foo
                if true {}
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation on expression must throw unsupported`() {
        assertThrows<DPUnsupportedAnnotationException> {
            evalProgram("""
                @Foo
                42
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation on for loop must throw unsupported`() {
        assertThrows<DPUnsupportedAnnotationException> {
            evalProgram("""
                @Foo
                for 0..3 {}
            """.trimIndent())
        }
    }

    @Test
    fun `Annotation on return must throw unsupported`() {
        assertThrows<DPUnsupportedAnnotationException> {
            evalProgram("""
                fun f {
                    @Foo
                    return 1
                }
                f()
            """.trimIndent())
        }
    }

    // -------------------------------------------------------------------------
    // Exception carries annotation name
    // -------------------------------------------------------------------------

    @Test
    fun `Exception message references the annotation name on unsupported target`() {
        val ex = assertThrows<DPUnsupportedAnnotationException> {
            evalProgram("""
                @MyAnnotation
                42
            """.trimIndent())
        }

        assert(ex.message?.contains("MyAnnotation") == true)
    }
}