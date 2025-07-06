package drift.utils

import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*

fun evalProgram(source: String) : DrValue {
    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv()

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    var result: DrValue = DrNull

    for (statement in ast) {
        result = statement.eval(env)
    }

    return result
}

fun evalWithOutputs(source: String) : MutableList<String> {
    val output = mutableListOf<String>()

    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv().apply {
        define("test", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }

    return output
}

fun evalWithOutput(source: String) : String {
    return evalWithOutputs(source).last()
}