package drift.parser.expressions

import drift.ast.*
import drift.ast.Set
import drift.exceptions.DriftParserException
import drift.parser.Parser
import drift.parser.Token
import drift.parser.functions.parseLambda
import drift.parser.statements.parseBlock
import drift.runtime.DrBool
import drift.runtime.DrInt
import drift.runtime.DrNull
import drift.runtime.DrString

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
                left = parseDriftIfOrTernary(left)

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

internal fun Parser.parsePrimary() : DrExpr {
    return when (val token = current()) {
        is Token.StringLiteral -> { advance(); Literal(DrString(token.value)) }
        is Token.IntLiteral -> { advance(); Literal(DrInt(token.value)) }
        is Token.BoolLiteral -> { advance(); Literal(DrBool(token.value)) }
        is Token.NullLiteral -> { advance(); Literal(DrNull) }
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
            else -> throw DriftParserException("Unexpected token ${token.value}")
        }
        else -> throw DriftParserException("Unexpected token $token")
    }
}

internal fun Parser.parseUnary() : DrExpr {
    val token = current()

    if (token is Token.Symbol && token.value in listOf("!", "-")) {
        val op = token.value

        advance()

        val right = parseUnary()

        return Unary(op, right)
    }

    return parsePrimary()
}

internal fun Parser.parseCallOrVariable() : DrExpr {
    val id = current() as Token.Identifier
    var expression: DrExpr = Variable(id.value)

    advance()

    while (checkSymbol("(")) {
        expression = parseCallArguments(expression)
    }

    return expression
}

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

internal fun Parser.parseDriftIfOrTernary(condition: DrExpr) : DrExpr {
    val thenBlock: Any = parseDriftIfOrTernaryBranch()
    var elseBlock: Any? = null

    if (matchSymbol(":")) {
        elseBlock = parseDriftIfOrTernaryBranch()
    }

    return when {
        thenBlock is DrStmt && (elseBlock == null || elseBlock is DrStmt) ->
            Conditional(condition, thenBlock, elseBlock as? DrStmt)
        thenBlock is ExprStmt && (elseBlock == null || elseBlock is ExprStmt) ->
            Ternary(
                condition,
                (thenBlock).expr,
                (elseBlock as? ExprStmt)?.expr)
        thenBlock is ExprStmt && elseBlock == null ->
            Ternary(condition, thenBlock.expr, null)
        else -> throw DriftParserException("Invalid Drift IF/ELSE branches")
    }
}

internal fun Parser.parseDriftIfOrTernaryBranch(): Any {
    return when (current()) {
        is Token.Symbol -> if (checkSymbol("{")) {
            parseBlock()
        } else {
            ExprStmt(parseExpression())
        }
        else -> ExprStmt(parseExpression())
    }
}