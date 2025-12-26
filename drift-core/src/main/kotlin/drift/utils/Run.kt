/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.utils

import drift.ast.statements.Function
import drift.runtime.evaluators.eval
import drift.checkers.collectors.SymbolCollector
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
import drift.runtime.values.specials.DrNull
import project.ProjectConfig
import project.ProjectStructure


/******************************************************************************
 * TEST UTIL FUNCTIONS
 *
 * Utility functions for testing purposes.
 ******************************************************************************/



val testConfig: ProjectConfig = ProjectConfig(
    name = "Test Project",
    structure = ProjectStructure(
        root = ".",
        "test"
    )
)



/**
 * Fully evaluates the given source code by lexing, parsing,
 * type-checking, and running it in a fresh environment.
 *
 * @param source Source code to evaluate entirely
 * @return The last evaluated value
 */
fun evalProgram(source: String) : DrValue {
    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv()

    env.apply {
        defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", DrClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("String", DrClass("String", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
    }
        
    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    var result: DrValue = DrNull

    for (statement in ast) {
        result = statement.eval(env)
    }

    return result
}



/**
 * Fully evaluates the given source code by lexing, parsing,
 * type-checking, and running it in a fresh environment.
 *
 * This function defines a native `test(value)` function which
 * that appends stringified values to a local output list.
 *
 * This variant allows multiple output values,
 * using the `test()` function.
 *
 * @param source Source code to evaluate entirely
 * @return All `test()` outputs
 */
fun evalWithOutputs(source: String) : MutableList<String> {
    val output = mutableListOf<String>()

    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv()
    env.apply {
        define("test", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType)
        )

        defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", DrClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
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
                        }))), closure = env))
        defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }

    return output
}



/**
 * Fully evaluates the given source code by lexing, parsing,
 * type-checking, and running it in a fresh environment.
 *
 * This function defines a native `test(value)` function which
 * that appends stringified values to a local output list.
 *
 * This variant allows once output value,
 * using the `test()` function.
 *
 * @param source Source code to evaluate entirely
 * @return The last `test()` output
 */
fun evalWithOutput(source: String) : String {
    return evalWithOutputs(source).last()
}



fun evalAndGetEnv(source: String) : DrEnv {
    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv()
    env.apply {
        defineClass("Int", DrClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", DrClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", DrClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
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
                        }))), closure = env))
        defineClass("Bool", DrClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }

    return env
}