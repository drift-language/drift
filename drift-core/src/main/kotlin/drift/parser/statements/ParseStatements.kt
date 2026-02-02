/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.statements

import drift.ast.bindings.ForVariable
import drift.ast.expressions.Lambda
import drift.ast.expressions.Literal
import drift.ast.statements.*
import drift.lexer.Token
import drift.parser.Parser
import drift.parser.callables.parseFunction
import drift.parser.classes.parseClass
import drift.parser.exceptions.DPMissingExpectedTokenException
import drift.parser.exceptions.DPStaticFieldMustBeInitializedException
import drift.parser.exceptions.DPUnallowedVariableInjectionPrefixUsageException
import drift.parser.exceptions.DPUnterminatedBlockException
import drift.parser.expressions.parseExpression
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.ParserType
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserVoid


/******************************************************************************
 * DRIFT STATEMENTS PARSER METHODS
 *
 * All methods permitting to parse statements are defined in this file.
 ******************************************************************************/



/**
 * Parse a statement expression.
 *
 * This method permits to dispatch to the corresponding
 * parsing method for the provided statement expression.
 *
 * @return Constructed statement AST object
 */
internal fun Parser.parseStatement() : ParserStatement {
    return when (val token = current()) {
        is Token.Symbol -> when (token.value) {
            "{" -> {
                advance(false)
                parseBlock()
            }
            else -> ExprStmt(parseExpression())
        }
        is Token.Identifier -> when {
            token.isKeyword(Token.Keyword.IF) -> {
                advance(false)
                parseClassicIf()
            }
            token.isKeyword(Token.Keyword.FUNCTION) -> {
                advance(false)
                parseFunction()
            }
            token.isKeyword(Token.Keyword.RETURN) -> {
                advance(false)
                parseReturn()
            }
            token.isKeyword(Token.Keyword.LEAVE) -> {
                advance(false)
                Return(Literal(ParserVoid))
            }
            token.isKeyword(Token.Keyword.FOR) -> {
                advance(false)
                parseFor()
            }
            token.isKeyword(Token.Keyword.CLASS) -> {
                advance(false)
                parseClass()
            }
            token.isKeyword(Token.Keyword.IMMUTLET) -> {
                advance(false)
                parseLet(false)
            }
            token.isKeyword(Token.Keyword.MUTLET) -> {
                advance(false)
                parseLet(true)
            }
            token.isKeyword(Token.Keyword.IMPORT) -> {
                advance(false)
                parseImport()
            }
            else -> ExprStmt(parseExpression())
        }
        else -> ExprStmt(parseExpression())
    }
}



/**
 * Attempt to parse a variable declaration expression
 *
 * ```
 * let immutable = value
 * var mutable = value
 * ```
 *
 * @param isMutable If the variable to declare is mutable
 * @return Constructed variable declaration AST object
 * @throws DPUnallowedVariableInjectionPrefixUsageException
 * @throws DPStaticFieldMustBeInitializedException
 */
internal fun Parser.parseLet(isMutable: Boolean, acceptUnassigned: Boolean = true) : Let {
    val injectedVariablePrefix = '$'

    val nameToken = expect<Token.Identifier>("variable name")
    val name = nameToken.value

    if (name.first() == injectedVariablePrefix)
        throw DPUnallowedVariableInjectionPrefixUsageException()

    advance(peekSymbol(":", true)
            || peekSymbol("=", true))

    // Type definition
    val type : ParserType = if (matchSymbol(":")) {
        parseType()
    } else {
        AnyType
    }

    if (peekSymbol("=", ignoreNewLines = true))
        advance()

    // Value initialization
    var expr = if (matchSymbol("=")) {
        parseExpression()
    } else if (!acceptUnassigned) {
        throw DPStaticFieldMustBeInitializedException(
            fieldName = name)
    } else {
        Literal(ParserNotAssigned)
    }

    if (expr is Lambda) {
        expr = expr.copy(name = name)
    }

    return Let(name, type, expr, isMutable)
}



/**
 * Parse a classic conditional statement expression
 *
 * ```
 * if condition {
 *
 * } else {
 *
 * }
 * ```
 *
 * @return Constructed classic conditional statement
 * AST object
 */
internal fun Parser.parseClassicIf() : If {
    val condition = parseExpression()

    skip(Token.NewLine)

    val thenBlock = parseStatement()
    var elseBlock: ParserStatement? = null

    if (current() is Token.Identifier
        && (current() as Token.Identifier).isKeyword(Token.Keyword.ELSE)) {

        advance()
        elseBlock = parseStatement()
    }

    return If(condition, thenBlock, elseBlock)
}



/**
 * Parse a return statement expression
 *
 * ```
 * return value
 * ```
 *
 * @return Constructed return statement AST object
 */
internal fun Parser.parseReturn() : Return =
    Return(parseExpression())



/**
 * Attempt to parse a block statement expression
 *
 * ```
 * {
 *      statement
 *      statement2
 * }
 * ```
 *
 * @return Constructed block statement AST object
 * @throws DPMissingExpectedTokenException
 */
internal fun Parser.parseBlock() : Block {
    skip(Token.NewLine)

    val statements = mutableListOf<ParserStatement>()

    while (true) {
        if (matchSymbol("}"))
            break

        val statement = parseStatement()
        statements.add(statement)

        if (current() == null)
            throw DPUnterminatedBlockException()

        when (val next = current()) {
            is Token.NewLine -> advance()
            is Token.Symbol -> if (next.value != "}")
                throw DPMissingExpectedTokenException(
                    expected = "newline or '}'",
                    found = next)
            else ->
                throw DPMissingExpectedTokenException(
                    expected = "newline or '}'",
                    found = next)
        }
    }

    return Block(statements)
}



/**
 * Attempt to parse a for statement expression
 *
 * ```
 * for iterable { as x
 *      statement
 * }
 * ```
 *
 * @return Constructed "for" statement AST object
 * @throws DPMissingExpectedTokenException
 */
internal fun Parser.parseFor() : For {
    val iterable = parseExpression()

    expectSymbol("{")
    skip(Token.NewLine)

    val variables = mutableListOf<ForVariable>()

    val c = current()

    if (c is Token.Identifier && c.isKeyword(Token.Keyword.AS)) {
        advance(false)

        do {
            variables.add(parseForVariable())

            advance(false)
        } while (matchSymbol(","))

        advance()
    }

    val statements = mutableListOf<ParserStatement>()

    while (!checkSymbol("}")) {
        statements.add(parseStatement())

        when (val c = current()) {
            is Token.NewLine -> advance()

            is Token.Symbol ->
                if (c.value == "}") break
                else throw DPMissingExpectedTokenException(
                    expected = "newline or '}'",
                    found = c)

            else -> throw DPMissingExpectedTokenException(
                    expected = "newline or '}'",
                    found = c)
        }
    }

    expectSymbol("}")

    return For(iterable, variables, Block(statements))
}

internal fun Parser.parseForVariable() : ForVariable {
    val name = expect<Token.Identifier>("variable name").value

    return ForVariable(name)
}



internal fun Parser.parseImport() : Import {
    val namespaceSteps = mutableListOf<String>()
    var importParts: MutableList<ImportPart>? = null
    var alias: String? = null
    var wildcard = false

    var c: Token?

    do {
        namespaceSteps.add(expect<Token.Identifier>("Invalid namespace").value)
        advance(false)
    } while (matchSymbol("."))

    c = current()

    if (matchSymbol("{")) {
        importParts = mutableListOf()

        do {
            if (matchSymbol("*")) {
                wildcard = true
            } else {
                val partName = expect<Token.Identifier>("variable name")

                advance(false)

                c = current()
                var partAlias: Token.Identifier? = null

                if (c is Token.Identifier && c.isKeyword(Token.Keyword.AS)) {
                    advance(false)
                    partAlias = expect<Token.Identifier>("variable name")
                    advance(false)
                }

                importParts.add(ImportPart(partName.value, partAlias?.value))
            }
        } while (matchSymbol(","))

        expectSymbol("}", advanceOnSuccess = true)
    } else if (c is Token.Identifier && c.isKeyword(Token.Keyword.AS)) {
        advance(false)
        alias = expect<Token.Identifier>("Invalid alias").value
        advance(false)
    }

    return Import(
        namespaceSteps.joinToString("."),
        namespaceSteps,
        alias,
        importParts,
        wildcard)
}