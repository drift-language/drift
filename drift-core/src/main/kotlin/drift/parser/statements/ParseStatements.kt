/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.statements

import drift.ast.expressions.Lambda
import drift.ast.expressions.Literal
import drift.ast.statements.Block
import drift.ast.statements.DrStmt
import drift.ast.statements.ExprStmt
import drift.ast.statements.For
import drift.ast.statements.If
import drift.ast.statements.Import
import drift.ast.statements.Let
import drift.ast.statements.Return
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.classes.parseClass
import drift.parser.expressions.parseExpression
import drift.parser.callables.parseFunction
import drift.parser.types.parseType
import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.specials.DrVoid


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
internal fun Parser.parseStatement() : DrStmt {
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
                Return(Literal(DrVoid))
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
 * @throws DriftParserException If the variable name is not found
 */
internal fun Parser.parseLet(isMutable: Boolean) : Let {
    val nameToken = expect<Token.Identifier>("Expected variable name")
    val name = nameToken.value

    advance(peekSymbol(":", true)
            || peekSymbol("=", true))

    // Type definition
    val type : DrType = if (matchSymbol(":")) {
        parseType()
    } else {
        AnyType
    }

    if (peekSymbol("=", ignoreNewLines = true))
        advance()

    // Value initialization
    var expr = if (matchSymbol("=")) {
        parseExpression()
    } else {
        Literal(DrNotAssigned)
    }

    if (expr is Lambda) {
        expr = expr.copy(name)
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
    var elseBlock: DrStmt? = null

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
 * @throws DriftParserException Many cases may
 * throw this exception:
 * - If the '{' character is not found
 * - If the block is unterminated
 * - If two statements are not separated by a newline
 * or a '}' symbol
 */
internal fun Parser.parseBlock() : Block {
    skip(Token.NewLine)

    val statements = mutableListOf<DrStmt>()

    while (true) {
        if (matchSymbol("}"))
            break

        val statement = parseStatement()
        statements.add(statement)

        if (current() == null)
            throw DriftParserException("Unterminated block, expected '}'")

        when (val next = current()) {
            is Token.NewLine -> advance()
            is Token.Symbol -> if (next.value != "}") {
                throw DriftParserException("Expected newline or '}' after statement but found $next")
            }
            else -> {
                throw DriftParserException("Expected newline or '}' after statement but found $next")
            }
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
 * @return Constructed for statement AST object
 * @throws DriftParserException Two cases may throw:
 * - If none variable follows the 'as' keyword
 * - If the for statement is not closed by the '}' symbol
 */
internal fun Parser.parseFor() : For {
    val iterable = parseExpression()

    expectSymbol("{")
    skip(Token.NewLine)

    val variables = mutableListOf<String>()

    val c = current()

    if (c is Token.Identifier && c.isKeyword(Token.Keyword.AS)) {
        advance(false)

        do {
            val name = expect<Token.Identifier>(
                "Expected variable name after '${Token.Keyword.AS}'").value

            variables.add(name)

            advance(false)
        } while (matchSymbol(","))

        advance()
    }

    val statements = mutableListOf<DrStmt>()

    while (!checkSymbol("}")) {
        statements.add(parseStatement())

        when (val c = current()) {
            is Token.NewLine -> advance()
            is Token.Symbol ->
                if (c.value == "}") break
                else throw DriftParserException("Expected newline or '}' after statement but found $c")
            else -> throw DriftParserException("Expected newline or '}' after statement but found $c")
        }
    }

    expectSymbol("}")

    return For(iterable, variables, Block(statements))
}



internal fun Parser.parseImport() : Import {
    val namespaceSteps = mutableListOf<String>()

    do {
        namespaceSteps.add(expect<Token.Identifier>("Invalid namespace").value)
        advance(false)
    } while (matchSymbol("."))

    return Import(
        namespaceSteps.joinToString("."),
        "${config.structure.root}/${namespaceSteps.joinToString("/")}.drift"
    )
}