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
    var left = parseUnary()

    while (true) {
        val opToken = current()

        if (opToken !is Token.Symbol) break

        // ----------------------
        // Function Call: foo(...)
        // ----------------------
        if (opToken.value == "(") {
            left = parseCallArguments(left)

            continue
        }

        // ----------------------
        // Access and Assign: obj.prop and obj.pro = x
        // ----------------------
        if (opToken.value == ".") {
            advance()

            val token = expect<Token.Identifier>("Expected property name after '.'")
            val propName = token.value

            advance()

            if (matchSymbol("=")) {
                val value = parseExpression(operatorPrecedence["="]!! + 1)

                return Set(left, propName, value)
            } else {
                left = Get(left, propName)

                val c = current()

                if (c is Token.Symbol
                    && (c.value in operatorPrecedence
                            || c.value in listOf(".", "(", "?"))) {

                    continue
                } else {
                    break
                }
            }
        }

        // ----------------------
        // Binary and Special Operators
        // ----------------------
        if (opToken.value in operatorPrecedence) {
            val precedence = operatorPrecedence[opToken.value] ?: 0

            if (precedence < minPrecedence) break

            val op = opToken.value

            advance()

            if (op == "=") {
                val value = parseExpression(precedence + 1)

                return when (left) {
                    is Variable -> Assign(left.name, value)
                    is Get -> Set(left.receiver, left.name, value)
                    else -> throw DriftParserException("Invalid assignment target")
                }
            } else if (op == "?") {
                left = parseDriftIf(left)

                continue
            }

            val right = parseExpression(precedence + 1)

            left = Binary(left, op, right)

            continue
        }

        break
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
        is Token.StringLiteral -> { advance(false); Literal(DrString(token.value)) }
        is Token.IntLiteral -> { advance(false); Literal(DrInt(token.value)) }
        is Token.BoolLiteral -> { advance(false); Literal(DrBool(token.value)) }
        is Token.NullLiteral -> { advance(false); Literal(DrNull) }
        is Token.Identifier -> parseCallOrVariable()
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
internal fun Parser.parseCallOrVariable() : DrExpr {
    val name = current() as Token.Identifier
    var expression: DrExpr = Variable(name.value)

    advance()

    while (checkSymbol("(")) {
        expression = parseCallArguments(expression)
    }

    return expression
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
    advance()

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

    skip(Token.NewLine)

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
    return when (current()) {
        is Token.Symbol -> if (checkSymbol("{")) {
            parseBlock()
        } else {
            ExprStmt(parseExpression())
        }
        else -> ExprStmt(parseExpression())
    }
}