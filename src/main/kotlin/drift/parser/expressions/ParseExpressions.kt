/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.expressions

import drift.ast.*
import drift.ast.Set
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.callables.parseLambda
import drift.parser.containers.parseList
import drift.parser.statements.parseBlock
import drift.runtime.values.primaries.DrBool
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrString
import drift.runtime.values.specials.DrNull


/******************************************************************************
 * DRIFT EXPRESSIONS PARSER METHODS
 *
 * All methods permitting to parse expressions are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse an expression
 *
 * This method dispatches to the corresponding parsing
 * method for the provided expression.
 *
 * @param minPrecedence Minimum operator priority index
 * @return Constructed expression AST object
 * @throws DriftParserException Many cases may throw:
 * - On object field access/assign:
 *   - If none identifier follows the dot '.'
 * - On binary and special operator assignment:
 *   - If target is neither a variable nor an object field
 */
internal fun Parser.parseExpression(minPrecedence: Int = 0) : DrExpr {
    return parseBinary(minPrecedence)
}



internal fun Parser.parseBinary(minPrecedence: Int) : DrExpr {
    var left = parseUnary()

    while (true) {
        if (checkSymbol("(")) {
            left = parseCallArguments(left)

            continue
        }

        val opToken = current() as? Token.Symbol ?: break
        val op = opToken.value

        if (matchSymbol(".")) {
            val prop = expect<Token.Identifier>("Expected property name after '.'")
                .value

            advance()

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
                    else -> throw DriftParserException("Invalid assignment target")
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
 * @throws DriftParserException If a token is unexpected
 */
internal fun Parser.parsePrimary() : DrExpr {
    return when (val token = current()) {
        is Token.StringLiteral -> {
            advance(false)
            Literal(DrString(token.value))
        }
        is Token.NumericLiteral -> {
            advance(false)
            Literal(token.value.run {
                toIntOrNull()?.let { DrInt(it) }
                ?: toLongOrNull()?.let { DrInt64(it) }
                ?: throw DriftParserException("Too long numeric ${token.value}")
            })
        }
        is Token.BoolLiteral -> {
            advance(false)
            Literal(DrBool(token.value))
        }
        is Token.NullLiteral -> {
            advance(false)
            Literal(DrNull)
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
            else -> throw DriftParserException("Unexpected token ${token.value}")
        }
        else -> throw DriftParserException("Unexpected token $token")
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
internal fun Parser.parseUnary() : DrExpr {
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
internal fun Parser.parseVariable() : DrExpr {
    val name = expect<Token.Identifier>("Expected variable name")

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
 * @throws DriftParserException If the parameters expression
 * is unterminated, without ')' symbol at end
 */
internal fun Parser.parseCallArguments(target: DrExpr) : DrExpr {
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
 * @throws DriftParserException Two cases may throw:
 * - If none name is provided to an argument
 * - If none '=' symbol is found
 */
internal fun Parser.parseArgument() : Argument {
    val token = current()

    if (token !is Token.Identifier) {
        throw DriftParserException("Expected parameter name for named argument")
    }

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
 * @return [Conditional] or [Ternary] AST object
 * @throws DriftParserException If a Drift-style conditional
 * expression branch is invalid
 */
internal fun Parser.parseDriftIf(condition: DrExpr) : DrExpr {
    val thenBlock: DrStmt = parseDriftIfBranch()
    var elseBlock: DrStmt? = null

    if (matchSymbol(":")) {
        elseBlock = parseDriftIfBranch()
    }

    if (thenBlock !is Block && thenBlock !is ExprStmt) {
        throw DriftParserException("Invalid Drift IF branch")
    } else if (elseBlock != null && elseBlock !is Block && elseBlock !is ExprStmt) {
        throw DriftParserException("Invalid Drift ELSE branch")
    }

    return Conditional(condition, thenBlock, elseBlock)
}



/**
 * Parse a Drift-style conditional expression or ternary
 * expression branch
 *
 * @return Constructed [Block] or [ExprStmt] AST object
 */
internal fun Parser.parseDriftIfBranch() : DrStmt {
    return when {
        matchSymbol("{") -> parseBlock()
        else -> ExprStmt(parseExpression())
    }
}