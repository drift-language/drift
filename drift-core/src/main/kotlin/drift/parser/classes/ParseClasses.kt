/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.classes

import drift.ast.expressions.Literal
import drift.ast.statements.Class
import drift.ast.statements.Func
import drift.ast.bindings.FunctionParameter
import drift.ast.metadata.Annotation
import drift.ast.statements.Let
import drift.ast.statements.ParserStatement
import drift.ast.statements.hooks.ParserHook
import drift.ast.statements.hooks.UnreturnableHook
import drift.parser.Parser
import drift.lexer.Token
import drift.parser.annotations.parseAnnotation
import drift.parser.callables.parseFunction
import drift.parser.callables.parseHook
import drift.parser.exceptions.DPOnlyOneConstructorPerClassException
import drift.parser.exceptions.DPOnlyOneStaticBlockPerClassException
import drift.parser.exceptions.DPUnexpectedStatementInClassBodyException
import drift.parser.exceptions.DPUnsupportedAnnotationException
import drift.parser.statements.parseLet
import drift.parser.statements.parseStatement
import drift.parser.types.parseType
import drift.runtime.VoidType
import drift.runtime.values.specials.ParserNotAssigned


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
 */
internal fun Parser.parseClass() : Class {
    val nameToken = expect<Token.Identifier>("class name")

    val annotations = storedAnnotations.toMutableList()
    storedAnnotations.clear()

    val name = nameToken.value
    val fields = mutableListOf<Let>()
    val methods = mutableListOf<Func>()
    val hooks = mutableListOf<ParserHook>()
    val staticFields = mutableListOf<Let>()
    val staticMethods = mutableListOf<Func>()
    val constructorParameters = mutableListOf<FunctionParameter>()

    var hasPrimaryConstructor = false
    var isStaticBlockAlreadyDefined = false


    fun hasConstructorHook() =
        hooks.firstOrNull { it.name == Token.Keyword.INIT.value } != null

    fun parseClassStatement() {
        when (val c = current()) {
            is Token.Identifier -> when {
                c.isKeyword(Token.Keyword.INIT) -> {
                    if (hasConstructorHook())
                        throw DPOnlyOneConstructorPerClassException()

                    hooks.add(parseHook(
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
                    if (isStaticBlockAlreadyDefined)
                        throw DPOnlyOneStaticBlockPerClassException()

                    isStaticBlockAlreadyDefined = true

                    advance()

                    expectSymbol("{", advanceOnSuccess = true)

                    while (!matchSymbol("}")) {
                        skip(Token.NewLine)

                        parseClassStaticBlock(staticFields, staticMethods)
                    }
                }

                else -> throw DPUnexpectedStatementInClassBodyException()
            }
            is Token.Annotation -> {
                val annotation = parseAnnotation()
                storedAnnotations.add(annotation)

                parseClassStatement()
            }

            else -> throw DPUnexpectedStatementInClassBodyException()
        }

        if (storedAnnotations.isNotEmpty())
            throw DPUnsupportedAnnotationException(annotationName = storedAnnotations.last().name)
    }


    advance(false)

    if (matchSymbol("(")) {
        hasPrimaryConstructor = true

        if (!checkSymbol(")")) {
            do {
                val paramToken = expect<Token.Identifier>("field name")

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
                    name = param.name,
                    type = param.type,
                    value = Literal(ParserNotAssigned),
                    isMutable = false))
            }

            hooks.add(UnreturnableHook(
                name = Token.Keyword.INIT.value,
                parameters = constructorParameters))
        }

        expectSymbol(")")
    }

    if (matchSymbol("{")) {
        while (!matchSymbol("}")) {
            skip(Token.NewLine)

            parseClassStatement()

            advance()
        }
    }

    if (hasConstructorHook())
        hooks.add(UnreturnableHook(name = Token.Keyword.INIT.value))

    return Class(
        name = name,
        annotations = annotations,
        fields = fields,
        methods = methods,
        hooks = hooks,
        staticFields = staticFields,
        staticMethods = staticMethods,
        hasPrimaryConstructor = hasPrimaryConstructor)
}