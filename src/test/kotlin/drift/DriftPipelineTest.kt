package drift

import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.AnyType
import drift.runtime.DrEnv
import drift.runtime.NullType
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class DriftPipelineTest {

    private fun evalSource(source: String) : DrEnv {
        val env = DrEnv().apply {
            defineClass("Int", DrClass("Int", emptyList(), emptyList()))
            defineClass("String", DrClass("String", emptyList(), emptyList()))
            defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
        }
        val tokens = lex(source)
        val ast = Parser(tokens).parse()

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        for (stmt in ast) {
            stmt.eval(env)
        }

        return env
    }

    @Test
    fun `Class declaration`() {
        val env = evalSource("""
            class User(name: String)
            let u: User = User("Bob")
        """.trimIndent())

        assertNotNull(env.resolveClass("User"))
    }

    @Test
    fun `Global function`() {
        val env = evalSource("""
            fun add(a: Int, b: Int) : Int {
                return a + b
            }
        """.trimIndent())

        assertNotNull(env.resolve("add"))
    }

    @Test
    fun `Global variable`() {
        val env = evalSource("""
            let x: Int = 42
        """.trimIndent())

        assertNotNull(env.resolve("x"))
    }
}