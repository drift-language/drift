/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.utils

import drift.ast.statements.Func
import drift.checkers.TypeChecker
import drift.checkers.collectors.SymbolCollector
import drift.lexer.lex
import drift.parser.Parser
import drift.runtime.*
import drift.runtime.evaluators.eval
import drift.runtime.values.callables.ParserMethod
import drift.runtime.values.callables.ParserNativeFunction
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNull
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
fun evalProgram(source: String) : ParserValue {
    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    val env = DrEnv()

    env.apply {
        defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", ParserClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("String", ParserClass("String", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
    }
        
    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    var result: ParserValue = ParserNull

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
        define("test", ParserNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                ParserNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType)
        )

        defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", ParserClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
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
                            val instance = receiver as ParserString

                            ParserInt(instance.value.length)
                        }))), closure = env))
        defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
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
        defineClass("Int", ParserClass("Int", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("Int64", ParserClass("Int64", mutableMapOf(), mutableMapOf(), closure = env))
        defineClass("UInt", ParserClass("UInt", mutableMapOf(), mutableMapOf(), closure = env))
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
                        impl = { receiver, args ->
                            val instance = receiver as ParserString

                            ParserInt(instance.value.length)
                        }))), closure = env))
        defineClass("Bool", ParserClass("Bool", mutableMapOf(), mutableMapOf(), closure = env))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }

    return env
}