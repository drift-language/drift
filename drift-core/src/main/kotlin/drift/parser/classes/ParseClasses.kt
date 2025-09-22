/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.classes

import drift.ast.Class
import drift.ast.Function
import drift.ast.FunctionParameter
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.callables.parseFunction
import drift.parser.types.parseType
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * DRIFT CLASSES PARSER METHODS
 *
 * All methods permitting to parse classes are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse a class definition expression
 *
 * ```
 * class U(field: Type) {
 *      statement
 * }
 * ```
 *
 * @return Constructed class definition statement
 * AST object
 * @throws DriftParserException Many cases may throw:
 * - If none class name is provided
 * - If none name is provided for a field, `class U(: Type)`
 * for example
 * - If non-method statement is defined into the class body
 */
internal fun Parser.parseClass() : Class {
    val nameToken = expect<Token.Identifier>("Expected class name")
    val name = nameToken.value
    val fields = mutableListOf<FunctionParameter>()
    val methods = mutableListOf<Function>()
    val staticFields = mutableListOf<FunctionParameter>()
    val staticMethods = mutableListOf<Function>()

    advance(false)

    if (matchSymbol("(")) {
        if (!checkSymbol(")")) {
            do {
                val paramToken = expect<Token.Identifier>("Expected field name")

                advance()

                expectSymbol(":")
                val fieldType = parseType()

                fields.add(FunctionParameter(paramToken.value, true, fieldType))
            } while (matchSymbol(","))
        }

        expectSymbol(")")
    }

    if (matchSymbol("{")) {
        while (!matchSymbol("}")) {
            skip(Token.NewLine)

            val c = current()

            if (c is Token.Identifier) {
                when {
                    c.isKeyword(Token.Keyword.FUNCTION) -> {
                        advance()

                        methods.add(parseFunction())
                    }
                    c.isKeyword(Token.Keyword.STATIC) -> {
                        advance()

                        expectSymbol("{")

                        while (!matchSymbol("}")) {
                            skip(Token.NewLine)

                            val subCurrent = current()

                            if (subCurrent is Token.Identifier && subCurrent.isKeyword(Token.Keyword.FUNCTION)) {
                                when {
                                    c.isKeyword(Token.Keyword.FUNCTION) -> {
                                        advance()

                                        staticMethods.add(parseFunction())
                                    }
                                }
                            }
                        }
                    }
                    else -> throw DriftParserException("Only methods are allowed inside class body")
                }
            } else {
                throw DriftParserException("Only methods are allowed inside class body")
            }

            if (current() is Token.NewLine) advance()
        }
    }

    return Class(name, fields, methods, staticFields, staticMethods)
}