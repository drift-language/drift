package drift

import drift.ast.eval
import drift.check.SymbolCollector
import drift.check.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.DrEnv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull

class DriftPipelineTest {

    private fun evalSource(source: String) : DrEnv {
        val env = DrEnv()
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