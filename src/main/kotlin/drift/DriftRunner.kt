package drift

import drift.ast.eval
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: drift <file.drift>")
        return
    }

    val file = File(args[0])

    if (!file.exists()) {
        println("File not found: ${args[0]}")
        return
    }

    val source = file.readText()

    val tokens = lex(source)
    val statements = Parser(tokens).parse()
    val env = DrEnv()

    env.define("print", DrNativeFunction(
        impl = { args ->
            println(args.joinToString(" ") { it.second.asString() })
            DrNull
        },
        paramTypes = listOf(AnyType),
        returnType = NullType
    ))

    for (statement in statements) {
        statement.eval(env)
    }
}