/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.utils

import drift.runtime.evaluators.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.*
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNull


/******************************************************************************
 * TEST UTIL FUNCTIONS
 *
 * Utility functions for testing purposes.
 ******************************************************************************/



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
        defineClass("Int", DrClass("Int", emptyList(), emptyList()))
        defineClass("Int64", DrClass("Int64", emptyList(), emptyList()))
        defineClass("UInt", DrClass("UInt", emptyList(), emptyList()))
        defineClass("String", DrClass("String", emptyList(), emptyList()))
        defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
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
    val env = DrEnv().apply {
        define("test", DrNativeFunction(
            impl = { _, args ->
                args.map { output.add(it.second.asString()) }
                DrNull
            },
            paramTypes = listOf(AnyType),
            returnType = NullType)
        )

        defineClass("Int", DrClass("Int", emptyList(), emptyList()))
        defineClass("Int64", DrClass("Int64", emptyList(), emptyList()))
        defineClass("UInt", DrClass("UInt", emptyList(), emptyList()))
        defineClass("String", DrClass("String", emptyList(), emptyList()))
        defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
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