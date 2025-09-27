/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime

import drift.ast.statements.Function
import drift.ast.statements.Import
import drift.runtime.evaluators.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftRuntimeException
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrVoid
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
            define("print", DrNativeFunction(
                impl = { _, args ->
                    println(args.joinToString(" ") { it.second.asString() })
                    DrVoid
                },
                paramTypes = listOf(AnyType),
                returnType = NullType))

            defineClass("String", DrClass(
                "String", emptyList(), listOf(
                    DrMethod(
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
                            })))))

            defineClass("Int", DrClass("Int", emptyList(), emptyList()))
            defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
            defineClass("Int64", DrClass("Int64", emptyList(), emptyList()))
            defineClass("UInt", DrClass("UInt", emptyList(), emptyList()))
        }

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        for (statement in ast) {
            if (statement is Import) statement.eval(ModuleLoader(
                config,
                projectDir,
                env))
            else statement.eval(env)
        }
    }
}