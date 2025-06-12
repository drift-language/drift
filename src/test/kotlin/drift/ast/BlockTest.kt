package drift.ast

import drift.parser.Parser
import drift.parser.lex
import drift.runtime.DrEnv
import drift.runtime.DrFunction
import drift.runtime.DrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BlockTest {
    @Test
    fun `Parse and eval block with two print statements`() {
        val input = """
            {
                print("Hello")
                print("World")
            }
        """.trimIndent()

        val tokens = lex(input)
        val parser = Parser(tokens)
        val statement = parser.parse()

        val output = mutableListOf<String>()
        val env = DrEnv().apply {
            define("print", DrFunction { args ->
                output.add(args.joinToString(" ") { it.asString() })
                DrNull
            })
        }

        statement.eval(env)

        assertEquals(listOf("Hello", "World"), output)
    }
}