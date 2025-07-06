package drift.ast

import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
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
            define(
                "print", DrNativeFunction(
                    impl = { _, args ->
                        args.map { output.add(it.second.asString()) }
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }

        statement.forEach { it.eval(env) }

        assertEquals(listOf("Hello", "World"), output)
    }
}