/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime

import drift.ast.statements.Func
import drift.ast.statements.Import
import drift.checkers.TypeChecker
import drift.checkers.collectors.SymbolCollector
import drift.lexer.lex
import drift.parser.Parser
import drift.runtime.evaluators.eval
import drift.runtime.exceptions.DRUnsuccessfulCastException
import drift.runtime.values.callables.ParserMethod
import drift.runtime.values.callables.ParserNativeFunction
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserVoid
import project.ProjectConfig
import java.io.File


/******************************************************************************
 * DRIFT RUNTIME
 *
 * Drift Runtime main singleton.
 ******************************************************************************/



object DriftRuntime {
    fun run(source: String, config: ProjectConfig, projectDir: File) {
        val env = DrEnv()

        val tokens = lex(source)
        val ast = Parser(tokens).parse()

        env.run {
            define("print", ParserNativeFunction(
                impl = { _, args ->
                    println(args.joinToString(" ") { it.second.asString() })
                    ParserVoid
                },
                paramTypes = listOf(AnyType),
                returnType = NullType))

            defineClass("String", ParserClass(
                "String", mutableMapOf(), mutableMapOf(
                    "length" to ParserMethod(
                        let = Func(
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
                            impl = { receiver, _ ->
                                val instance = receiver as? ParserString
                                    ?: throw DRUnsuccessfulCastException(
                                        valueType = receiver?.type() ?: AnyType,
                                        expectedType = ObjectType("String"))

                                ParserInt(instance.value.length)
                            }))), closure = env))

            defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("Int64", ParserClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
            defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
        }

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        val loader = ModuleLoader(
            config,
            projectDir,
            env)

        for (statement in ast) {
            if (statement is Import) statement.eval(loader)
            else statement.eval(env)
        }
    }
}