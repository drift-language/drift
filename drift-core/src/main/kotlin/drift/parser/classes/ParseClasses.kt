/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.classes

import drift.ast.expressions.Assign
import drift.ast.expressions.Literal
import drift.ast.expressions.Variable
import drift.ast.statements.Class
import drift.ast.statements.ExprStmt
import drift.ast.statements.Function
import drift.ast.statements.FunctionParameter
import drift.ast.statements.Let
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.callables.parseFunction
import drift.parser.callables.parseHook
import drift.parser.statements.parseLet
import drift.parser.types.parseType
import drift.runtime.UnknownType
import drift.runtime.VoidType
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.specials.DrVoid
import sun.invoke.util.BytecodeDescriptor.parseMethod


/******************************************************************************
 * DRIFT CLASSES PARSER METHODS
 *
 * All methods permitting parse classes are defined in this file.
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
 * - If a non-method statement is defined into the class body
 */
internal fun Parser.parseClass() : Class {
    val nameToken = expect<Token.Identifier>("Expected class name")
    val name = nameToken.value
    val fields = mutableListOf<Let>()
    val methods = mutableListOf<Function>()
    val staticFields = mutableListOf<Let>()
    val staticMethods = mutableListOf<Function>()
    val constructorParameters = mutableListOf<FunctionParameter>()
    var hasPrimaryConstructor = false

    advance(false)

    if (matchSymbol("(")) {
        hasPrimaryConstructor = true

        if (!checkSymbol(")")) {
            do {
                val paramToken = expect<Token.Identifier>("Expected field name")

                advance()

                expectSymbol(":")
                val fieldType = parseType()

                constructorParameters.add(FunctionParameter(
                    paramToken.value,
                    true,
                    fieldType))
            } while (matchSymbol(","))

            for (param in constructorParameters) {
                fields.add(Let(
                    param.name,
                    param.type,
                    Literal(DrNotAssigned),
                    isMutable = false))
            }

            methods.add(Function(
                Token.Keyword.INIT.value,
                constructorParameters,
                listOf(),
//                constructorParameters.map {
//                    ExprStmt(Assign(it.name, Variable(it.name)))        // TODO: vider et gérer avec runtime??
//                },
                VoidType))
        }

        expectSymbol(")")
    }

    var isStaticBlockAlreadyDefined = false

    if (matchSymbol("{")) {
        while (!matchSymbol("}")) {
            skip(Token.NewLine)

            val c = current()

            if (c is Token.Identifier) {
                when {
                    c.isKeyword(Token.Keyword.INIT) -> {
                        if (hasPrimaryConstructor) {
                            throw DriftParserException(
                                "A class cannot have both primary " +
                                "and standard constructors")
                        }

                        methods.add(parseHook(
                            forceParameters = true,
                            disableReturnStatement = true))
                    }
                    c.isKeyword(Token.Keyword.IMMUTLET) ||
                    c.isKeyword(Token.Keyword.MUTLET) -> {

                        advance(false)

                        fields += parseLet(
                            isMutable = c.isKeyword(Token.Keyword.MUTLET),
                            acceptUnassigned = true)
                    }
                    c.isKeyword(Token.Keyword.FUNCTION) -> {
                        advance()

                        methods.add(parseFunction())
                    }
                    c.isKeyword(Token.Keyword.STATIC) -> {
                        if (isStaticBlockAlreadyDefined) {
                            throw DriftParserException(
                                "A class cannot have more than " +
                                "one static block")
                        }

                        isStaticBlockAlreadyDefined = true

                        advance()

                        expectSymbol("{", advanceOnSuccess = true)

                        while (!matchSymbol("}")) {
                            skip(Token.NewLine)

                            parseClassStaticBlock(staticFields, staticMethods)
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

    return Class(name, fields, methods, staticFields, staticMethods, hasPrimaryConstructor = hasPrimaryConstructor)
}