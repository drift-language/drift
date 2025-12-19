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
import drift.checkers.collectors.SymbolCollector
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
                "String", mutableMapOf(), mutableMapOf(
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
                            })))))

            defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf()))
            defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf()))
            defineClass("Int64", DrClass("Int64", mutableMapOf(), mutableMapOf()))
            defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf()))
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