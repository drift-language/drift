/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import drift.DriftVersion
import drift.ast.statements.Function
import drift.checkers.TypeChecker
import drift.checkers.collectors.SymbolCollector
import drift.lexer.lex
import drift.parser.Parser
import drift.runtime.AnyType
import drift.runtime.DrEnv
import drift.runtime.NullType
import drift.runtime.ObjectType
import drift.runtime.evaluators.eval
import drift.runtime.values.callables.ParserMethod
import drift.runtime.values.callables.ParserNativeFunction
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserVoid
import project.loadConfig
import java.io.File

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

    if (args.isEmpty()) return

    t.run {
        println(bold("Entry: ${args[0]}"))
        println()
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
        define("print", ParserNativeFunction(
            impl = { _, args ->
                println(args.joinToString(" ") { it.second.asString() })
                ParserVoid
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        defineClass("String", ParserClass("String", mutableMapOf(), mutableMapOf(
            "length" to ParserMethod(
                let = Function(
                    name = "length",
                    parameters = emptyList(),
                    returnType = ObjectType("Int"),
                    body = emptyList()
                ),
                closure = env,
                nativeImpl = ParserNativeFunction(
                    name = "length",
                    paramTypes = emptyList(),
                    returnType = ObjectType("Int"),
                    impl = { receiver, args ->
                        val instance = receiver as ParserString

                        ParserInt(instance.value.length)
                    }
                )
            )
        ), closure = env))

        defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", ParserClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
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