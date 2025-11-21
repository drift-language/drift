/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.callables

import drift.ast.statements.Function
import drift.ast.statements.FunctionParameter
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.statements.parseBlock
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.VoidType


/******************************************************************************
 * DRIFT HOOK PARSER METHOD
 *
 * Method permitting parse hooks.
 ******************************************************************************/



/**
 * Attempt to parse a hook definition expression.
 *
 * A hook is a special function that does not respect the classical
 * syntax ``fun name``. A hook is a reserved function used by Drift
 * natively to do many tasks like construct class instances.
 *
 * ```
 * class A {
 *     private let value1: Type
 *
 *     init (value1: Type) {
 *         $this.value1 = value1
 *     }
 * }
 * ```
 */
internal fun Parser.parseHook(
    forceParameters: Boolean = false,
    disableReturnStatement: Boolean = false) : Function {

    val name = expect<Token.Identifier>("Expected hook name").value

    if (name !in authorizedHookNames)
        throw DriftParserException("Hook name '$name' is not allowed")

    advance(false)

    if (forceParameters && !checkSymbol("("))
        throw DriftParserException("Hook '$name' requires parameters")

    val parameters = mutableListOf<FunctionParameter>()

    if (matchSymbol("(")) {
        if (!checkSymbol(")")) {
            do {
                val paramName = expect<Token.Identifier>("Expected hook parameter name").value

                advance(false)

                expectSymbol(":")

                val type = parseType()

                parameters += FunctionParameter(
                    paramName,
                    true,
                    type)
            } while (matchSymbol(","))
        }

        expectSymbol(")")
    }

    var hookReturnType: DrType = VoidType

    if (matchSymbol(":")) {
        if (disableReturnStatement)
            throw DriftParserException("Hook '$name' cannot return a value")

        hookReturnType = parseType()
    }

    expectSymbol("{")

    val body = parseBlock()

    return Function(
        name,
        parameters,
        body.statements,
        hookReturnType)
}