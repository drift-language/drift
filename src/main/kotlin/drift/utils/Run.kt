package drift.utils

import drift.ast.eval
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*

fun evalProgram(source: String) : DrValue {
    val tokens = lex(source)
    val statements = Parser(tokens).parse()
    val env = DrEnv()

    var result: DrValue = DrNull

    for (statement in statements) {
        result = statement.eval(env)
    }

    return result
}

fun evalWithOutputs(source: String) : MutableList<String> {
    val output = mutableListOf<String>()

    val tokens = lex(source)
    val statements = Parser(tokens).parse()
    val env = DrEnv().apply {
        define("test", DrNativeFunction(
            impl = { args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType))
    }

    for (statement in statements) {
        statement.eval(env)
    }

    return output
}

fun evalWithOutput(source: String) : String {
    return evalWithOutputs(source).last()
}