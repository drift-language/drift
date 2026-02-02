/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.expressions

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.Block
import drift.ast.statements.ParserStatement
import drift.ast.statements.ExprStmt
import drift.lexer.Token
import drift.parser.Parser
import drift.parser.callables.parseLambda
import drift.parser.containers.parseList
import drift.parser.exceptions.DPInvalidAssignmentTargetException
import drift.parser.exceptions.DPInvalidDriftConditionalBranchException
import drift.parser.exceptions.DPNamedArgumentMustBeNamedException
import drift.parser.exceptions.DPNumericSizeOverflowException
import drift.parser.exceptions.DPUnexpectedExpressionException
import drift.parser.exceptions.DPUnexpectedSymbolException
import drift.parser.statements.parseBlock
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNull


/******************************************************************************
 * DRIFT EXPRESSIONS PARSER METHODS
 *
 * All methods permitting parsing expressions are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse an expression.
 *
 * This method dispatches to the corresponding parsing
 * method for the provided expression.
 *
 * @param minPrecedence Minimum operator priority index
 * @return Constructed expression AST object
 */
internal fun Parser.parseExpression(minPrecedence: Int = 0) : ParserExpression {
    return parseBinary(minPrecedence)
}



/**
 * Attempt to parse a binary expression.
 *
 * This method dispatches to the corresponding parsing
 * method for the provided expression.
 *
 * @param minPrecedence Minimum operator priority index
 * @return Constructed expression AST object
 * @throws DPInvalidAssignmentTargetException
 */
internal fun Parser.parseBinary(minPrecedence: Int) : ParserExpression {
    var left = parseUnary()

    while (true) {
        if (checkSymbol("(")) {
            left = parseCallArguments(left)

            continue
        }

        val opToken = current() as? Token.Symbol ?: break
        val op = opToken.value

        if (matchSymbol(".")) {
            val prop = expect<Token.Identifier>("member name after '.'")
                .value

            advance(false)

            if (matchSymbol("=")) {
                val v = parseExpression(operatorPrecedence["="]!! + 1)
                left = Set(left, prop, v)
            } else {
                left = Get(left, prop)
            }

            continue
        }

        val precedence = operatorPrecedence[op] ?: break

        if (precedence < minPrecedence) break

        advance()

        left = when (op) {
            // Handle special Drift conditional operator (right-associative)
            "?" -> parseDriftIf(left)

            "=" -> {
                val right = parseBinary(precedence + 1)

                when (left) {
                    is Variable -> Assign(left.name, right)
                    is Get -> Set(left.receiver, left.name, right)
                    else -> throw DPInvalidAssignmentTargetException()
                }
            }

            // Normal binary or assignment operator
            else -> {
                val right = parseBinary(precedence + 1)

                Binary(left, op, right)
            }
        }
    }

    return left
}




/**
 * Attempt to parse a primary expression.
 *
 * This method dispatches to the corresponding parsing
 * method for the provided primary expression.
 *
 * A primary expression is a native expression like a string,
 * an integer or a lambda, for example.
 *
 * ```
 * "Hello"
 * 1
 * true
 * () -> {}
 * ```
 *
 * @return Constructed literal or lambda or parentheses
 * expression AST object
 * @throws DPNumericSizeOverflowException
 */
internal fun Parser.parsePrimary() : ParserExpression {
    return when (val token = current()) {
        is Token.StringLiteral -> {
            advance(false)
            Literal(ParserString(token.value))
        }
        is Token.NumericLiteral -> {
            advance(false)
            Literal(token.value.run {
                toIntOrNull()?.let { ParserInt(it) }
                    ?: toLongOrNull()?.let { ParserInt64(it) }
                    ?: throw DPNumericSizeOverflowException()
            })
        }
        is Token.BoolLiteral -> {
            advance(false)
            Literal(ParserBool(token.value))
        }
        is Token.NullLiteral -> {
            advance(false)
            Literal(ParserNull)
        }
        is Token.Identifier -> parseVariable()
        is Token.Symbol -> when (token.value) {
            "(" -> {
                if (isLambda()) {
                    return parseLambda()
                }

                advance()

                val expression = parseExpression()
                expectSymbol(")")

                expression
            }
            "[" -> parseList()
            else -> throw DPUnexpectedSymbolException(
                unexpected = token,
                context = "in primary expression")
        }
        else -> throw DPUnexpectedExpressionException(
            unexpected = token,
            context = "in primary expression")
    }
}



/**
 * Parse an unary expression
 *
 * ```
 * -1
 * !booleanExpression
 * ```
 *
 * @return Constructed primary parsing result AST object
 */
internal fun Parser.parseUnary() : ParserExpression {
    val token = current()

    if (token is Token.Symbol && token.value in listOf("!", "-")) {
        val op = token.value

        advance(false)

        val right = parseUnary()

        return Unary(op, right)
    }

    return parsePrimary()
}



/**
 * Parse a callable call or variable access expression
 *
 * ```
 * variable      // Variable access
 * call()        // Callable call
 * ```
 *
 * @return Constructed variable access or callable call
 * value AST object
 */
internal fun Parser.parseVariable() : ParserExpression {
    val name = expect<Token.Identifier>("variable name")

    advance(false)

    return Variable(name.value)
}



/**
 * Attempt to parse a callable call arguments expression
 *
 * ```
 * call(x = 1, y = 2)   // With parameters names
 * call(1, 2)           // Without parameters names
 * ```
 *
 * @return Constructed callable call AST object
 */
internal fun Parser.parseCallArguments(target: ParserExpression) : ParserExpression {
    expectSymbol("(")

    val args = mutableListOf<Argument>()

    if (!checkSymbol(")")) {
        do {
            val c = current()
            val arg = if (c is Token.Identifier && peekSymbol("=")) {
                parseArgument()
            } else {
                Argument(null, parseExpression())
            }

            args.add(arg)
        } while (matchSymbol(","))
    }

    expectSymbol(")")

    return Call(target, args)
}



/**
 * Attempt to parse a named argument
 *
 * @return Call argument AST object
 * @throws DPNamedArgumentMustBeNamedException
 */
internal fun Parser.parseArgument() : Argument {
    val token = current()

    if (token !is Token.Identifier)
        throw DPNamedArgumentMustBeNamedException()

    val name = token.value
    advance()
    expectSymbol("=")

    val expr = parseExpression()

    return Argument(name, expr)
}



/**
 * Attempt to parse a Drift-style conditional expression
 * or a ternary expression
 *
 * ```
 * // Drift-style conditional
 * condition ? {
 *      ...
 * } : elseCondition ? {
 *      ...
 * } : {
 *      ...
 * }
 *
 * // Ternary
 * let a = condition ? 1 : 2
 * ```
 *
 * @param condition Condition expression
 * @return [Conditional] AST object
 * @throws DPInvalidDriftConditionalBranchException
 */
internal fun Parser.parseDriftIf(condition: ParserExpression) : ParserExpression {
    val thenBlock: ParserStatement = parseDriftIfBranch()
    val elseBlock: ParserStatement? =
        if (matchSymbol(":")) parseDriftIfBranch()
        else null

    if (thenBlock !is Block && thenBlock !is ExprStmt) {
        throw DPInvalidDriftConditionalBranchException(
            branchType = DPInvalidDriftConditionalBranchException.BranchType.IF)
    } else if (elseBlock != null && elseBlock !is Block && elseBlock !is ExprStmt) {
        throw DPInvalidDriftConditionalBranchException(
            branchType = DPInvalidDriftConditionalBranchException.BranchType.ELSE)
    }

    return Conditional(condition, thenBlock, elseBlock)
}



/**
 * Parse a Drift-style conditional expression or ternary
 * expression branch
 *
 * @return Constructed [Block] or [ExprStmt] AST object
 */
internal fun Parser.parseDriftIfBranch() : ParserStatement {
    return when {
        matchSymbol("{") -> parseBlock()
        else -> ExprStmt(parseExpression())
    }
}