package drift.cli

import drift.ast.Function
import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
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

    val env = DrEnv()

    val tokens = lex(source)
    val ast = Parser(tokens).parse()

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    env.run {
        define("print", DrNativeFunction(
            impl = { _, args ->
                println(args.joinToString(" ") { it.second.asString() })
                DrVoid
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        defineClass("String", DrClass("String", emptyList(), listOf(
            DrMethod(
                let = Function(
                    name = "length",
                    parameters = emptyList(),
                    returnType = ClassType("Int"),
                    body = emptyList()
                ),
                closure = env,
                nativeImpl = DrNativeFunction(
                    name = "length",
                    paramTypes = emptyList(),
                    returnType = ClassType("Int"),
                    impl = { receiver, args ->
                        println("DEBUG: nativeImpl receiver = ${receiver?.asString()} type=${receiver?.type()}")
                        println("DEBUG receiver javaClass = ${receiver?.javaClass?.name}")


                        val instance = receiver as? DrString
                            ?: throw DriftRuntimeException("length() called on non-String")

                        DrInt(instance.value.length)
                    }
                )
            )
        )))

        defineClass("Int", DrClass("Int", emptyList(), emptyList()))

        defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
    }

    for (statement in ast) {
        statement.eval(env)
    }
}