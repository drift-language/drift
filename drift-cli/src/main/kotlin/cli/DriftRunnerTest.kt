/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli

import drift.ast.statements.Function
import drift.runtime.evaluators.eval
import drift.checkers.collectors.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.AnyType
import drift.runtime.DrEnv
import drift.runtime.NullType
import drift.runtime.ObjectType
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrVoid
import project.ProjectConfig
import project.loadConfig
import java.io.File
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import drift.DriftVersion

fun main(args: Array<String>) {
    val t = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)

    t.run {
        println(bold(
            "-- Drift CommandLine Debugger Feature — ${DriftVersion.fullVersion} --"
        ))

        println()
        println(bold(
            (driftBlue)("Running ") +
            (rgb("#FFF") on driftBlue)(" Drift ") +
            (rgb("#FFF") on green)(" Debugger ")
        ))
    }

    if (args.isNotEmpty()) {
        t.run {
            println(bold("Entry: ${args[0]}"))
            println()
        }
    }

    val file = File(args[0])

    if (!file.exists()) {
        cliError("File not found: ${args[0]}", t)
    }

    val source = file.readText()
    val env = DrEnv()
    val config = loadConfig(File("examples"))

    val tokens = lex(source)
    t.println(bold(yellow("[TOKENS]\t")) + italic(tokens.toString()))

    val ast = Parser(tokens).parse()
    t.println(bold(red("[AST]\t\t")) + italic(ast.toString()))
    t.println("\n——————\n")

    env.run {
        define("print", DrNativeFunction(
            impl = { _, args ->
                println(args.joinToString(" ") { it.second.asString() })
                DrVoid
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        defineClass("String", DrClass("String", mutableMapOf(), mutableMapOf(
            "length" to DrMethod(
                let = Function(
                    name = "length",
                    parameters = emptyList(),
                    returnType = ObjectType("Int"),
                    body = emptyList()
                ),
                closure = env,
                nativeImpl = DrNativeFunction(
                    name = "length",
                    paramTypes = emptyList(),
                    returnType = ObjectType("Int"),
                    impl = { receiver, args ->
                        val instance = receiver as? DrString
                            ?: throw DriftRuntimeException("length() called on non-String")

                        DrInt(instance.value.length)
                    }
                )
            )
        )))

        defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf()))
        defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf()))
        defineClass("Int64", DrClass("Int64", mutableMapOf(), mutableMapOf()))
        defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf()))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }

    t.run {
        println()
        println(bold(
            (white on black)(" — End of Program — ")
        ))
    }
}