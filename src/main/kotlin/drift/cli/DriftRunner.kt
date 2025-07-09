/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.cli

import drift.ast.Function
import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrVoid
import java.io.File


/******************************************************************************
 * DRIFT RUNNER
 *
 * This runner file permits to evaluate and execute a Drift file.
 ******************************************************************************/



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