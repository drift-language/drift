package drift

import drift.ast.eval
import drift.check.SymbolCollector
import drift.check.TypeChecker
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
import java.io.File
import kotlin.system.exitProcess

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

    val env = DrEnv()

    val tokens = lex(source)
    val ast = Parser(tokens).parse()

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    env.define("print", DrNativeFunction(
        impl = { args ->
            println(args.joinToString(" ") { it.second.asString() })
            DrNull
        },
        paramTypes = listOf(AnyType),
        returnType = NullType
    ))

    for (statement in ast) {
        statement.eval(env)
    }
}