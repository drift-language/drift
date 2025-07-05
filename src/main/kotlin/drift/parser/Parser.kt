package drift.parser

import drift.ast.*
import drift.ast.Function
import drift.ast.Set
import drift.exceptions.DriftParserException
import drift.runtime.*

class Parser(private val tokens: List<Token>) {
    private var i = 0
    private var depth = 0

    private val operatorPrecedence: Map<String, Int> = mapOf(
        "="     to 1,

        "?"     to 2,

        "=="    to 3,
        "!="    to 3,
        ">"     to 3,
        "<"     to 3,
        ">="    to 3,
        "<="    to 3,

        "+"     to 4,
        "-"     to 4,

        "*"     to 5,
        "/"     to 5,
    )

    private fun current() : Token? = tokens.getOrNull(i)
    private fun advance() {
        i++

        val c = current()

        if (c is Token.Symbol) {
            if (c.value in listOf("(", "[", "{")) depth++
            else if (c.value in listOf(")", "]", "}")) depth--
        } else if (c is Token.NewLine && depth > 0) {
            advance()
        }
    }
    private fun isAtEnd() : Boolean = current() == Token.EOL

    fun parse(): List<DrStmt> {
        val statements = mutableListOf<DrStmt>()

        while (!isAtEnd()) {
            val token = current()

            if (token is Token.NewLine) {
                advance()
                continue
            }

            val statement = parseStatement()
            statements.add(statement)

            val next = current()

            if (next is Token.NewLine || next is Token.Symbol && next.value == "}") {
                advance()
            } else if (!isAtEnd()) {
                throw DriftParserException("Expected newline after top-level statement but found $next")
            }
        }

        return statements
    }

    private fun parseStatement() : DrStmt {
        return when (val token = current()) {
            is Token.Symbol -> when (token.value) {
                "{" -> parseBlock()
                else -> ExprStmt(parseExpression())
            }
            is Token.Identifier -> when {
                token.isKeyword(Token.Keyword.IF) -> parseClassicIf()
                token.isKeyword(Token.Keyword.FUNCTION) -> {
                    advance()
                    parseFunction()
                }
                token.isKeyword(Token.Keyword.RETURN) -> {
                    advance()
                    parseReturn()
                }
                token.isKeyword(Token.Keyword.CLASS) -> {
                    advance()
                    parseClass()
                }
                token.isKeyword(Token.Keyword.IMMUTLET) -> {
                    advance()
                    parseLet(false)
                }
                token.isKeyword(Token.Keyword.MUTLET) -> {
                    advance()
                    parseLet(true)
                }
                else -> ExprStmt(parseExpression())
            }
            else -> ExprStmt(parseExpression())
        }
    }

    private fun parseLet(isMutable: Boolean) : DrStmt {
        val nameToken = expect<Token.Identifier>("Expected variable name")
        val name = nameToken.value

        advance()

        val type : DrType = if (matchSymbol(":")) {
            parseType()
        } else {
            AnyType
        }

        val expr = if (matchSymbol("=")) {
            parseExpression()
        } else {
            Literal(DrNotAssigned)
        }

        return Let(name, type, expr, isMutable)
    }

    private fun parsePrimary() : DrExpr {
        return when (val token = current()) {
            is Token.StringLiteral -> { advance(); Literal(DrString(token.value)) }
            is Token.IntLiteral -> { advance(); Literal(DrInt(token.value)) }
            is Token.BoolLiteral -> { advance(); Literal(DrBool(token.value)) }
            is Token.NullLiteral -> { advance(); Literal(DrNull) }
            is Token.Identifier -> parseCallOrVariable()
            is Token.Symbol -> when (token.value) {
                "(" -> {
                    if (isLambda()) {
                        return parseAnonymousFunctionWithoutKeyword()
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

    private fun parseUnary() : DrExpr {
        val token = current()

        if (token is Token.Symbol && token.value in listOf("!", "-")) {
            val op = token.value

            advance()

            val right = parseUnary()

            return Unary(op, right)
        }

        return parsePrimary()
    }

    private fun parseExpression(minPrecedence: Int = 0) : DrExpr {
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

    private fun parseCallOrVariable() : DrExpr {
        val id = current() as Token.Identifier
        var expression: DrExpr = Variable(id.value)

        advance()

        while (checkSymbol("(")) {
            expression = parseCallArguments(expression)
        }

        return expression
    }

    private fun parseCallArguments(target: DrExpr) : DrExpr {
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

    private fun parseArgument() : Argument {
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

    private fun parseBlock() : Block {
        val open = current()

        if (open !is Token.Symbol || open.value != "{") {
            throw DriftParserException("Expected '{' but found $open")
        }

        advance()

        val statements = mutableListOf<DrStmt>()

        while (true) {
            val token = current()
                ?: throw DriftParserException("Unterminated block, expected '}'")

            if (token is Token.Symbol && token.value == "}") {
                advance()
                break
            }

            if (token is Token.NewLine) {
                advance()
                continue
            }

            val statement = parseStatement()
            statements.add(statement)

            val next = current()
            println("NEXT CHECK = $next")

            when (next) {
                is Token.NewLine -> advance()
                is Token.Symbol -> if (next.value != "}")
                    throw DriftParserException("Expected newline or '}' after statement but found $next")
                else -> throw DriftParserException("Expected newline or '}' after statement but found $next")
            }
        }

        return Block(statements)
    }

    private fun parseStatementOrBlock() : DrStmt {
        return if (current() is Token.Symbol && (current() as Token.Symbol).value == "{") {
            parseBlock()
        } else {
            ExprStmt(parseExpression())
        }
    }

    private fun parseClassicIf() : If {
        val token = current()

        if (token !is Token.Identifier || !token.isKeyword(Token.Keyword.IF)) {
            throw DriftParserException("Expected 'if' but found $token")
        }

        advance()

        val condition = parseExpression()
        val thenBlock = parseBlock()
        var elseBlock: DrStmt? = null

        if (current() is Token.Identifier
            && (current() as Token.Identifier).isKeyword(Token.Keyword.ELSE)) {

            advance()
            elseBlock = parseBlock()
        }

        return If(condition, thenBlock, elseBlock)
    }

    private fun parseDriftIfOrTernary(condition: DrExpr) : DrExpr {
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

    private fun parseDriftIfOrTernaryBranch(): Any {
        return when (current()) {
            is Token.Symbol -> if (checkSymbol("{")) {
                parseBlock()
            } else {
                ExprStmt(parseExpression())
            }
            else -> ExprStmt(parseExpression())
        }
    }

    private fun parseFunction() : Function {
        val nameToken = expect<Token.Identifier>("Expected function name")
        val name = nameToken.value
        val parameters = mutableListOf<FunctionParameter>()

        advance()

        if (matchSymbol("(")) {
            if (!checkSymbol(")")) {
                do {
                    val isPositional: Boolean = matchSymbol("*")
                    val paramToken = expect<Token.Identifier>("Expected parameter name")

                    if (parameters.firstOrNull { it.name == paramToken.value } != null)
                        throw DriftParserException("Parameter ${paramToken.value} is already defined")

                    advance()

                    var paramType: DrType = AnyType

                    if (matchSymbol(":")) {
                        paramType = parseType()
                    }

                    parameters.add(FunctionParameter(paramToken.value, isPositional, paramType))
                } while (matchSymbol(","))
            }

            expectSymbol(")")
        }

        val returnType: DrType =
            if (matchSymbol(":")) parseType()
            else AnyType

        val body = parseBlock().statements

        return Function(name, parameters, body, returnType)
    }

    private fun parseAnonymousFunctionWithoutKeyword() : DrExpr {
        expectSymbol("(")

        val parameters = mutableListOf<FunctionParameter>()

        if (!checkSymbol(")")) {
            do {
                val paramToken = expect<Token.Identifier>("Expected parameter name")

                if (parameters.firstOrNull { it.name == paramToken.value } != null)
                    throw DriftParserException("Parameter ${paramToken.value} is already defined")

                advance()

                val paramType = if (matchSymbol(":")) {
                    parseType()
                } else {
                    AnyType
                }

                parameters.add(FunctionParameter(paramToken.value, isPositional = true, paramType))
            } while (matchSymbol(","))
        }

        expectSymbol(")")

        var returnType: DrType = AnyType

        if (matchSymbol(":")) {
            returnType = parseType()
        }

        expectSymbol("->")

        val body = parseBlock().statements

        return Lambda(parameters, body, returnType)
    }

    private fun parseClass() : Class {
        val nameToken = expect<Token.Identifier>("Expected class name")
        val name = nameToken.value
        val fields = mutableListOf<FunctionParameter>()
        val methods = mutableListOf<Function>()

        advance()

        expectSymbol("(")

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

        if (matchSymbol("{")) {
            while (!checkSymbol("}")) {
                val c = current()

                if (c !is Token.Identifier || !c.isKeyword(Token.Keyword.FUNCTION)) {
                    throw DriftParserException("Only methods are allowed inside class body")
                }

                advance()

                methods.add(parseFunction())

                if (current() is Token.NewLine) advance()
            }
        }

        return Class(name, fields, methods)
    }

    private fun parseType() : DrType {
        val token = expect<Token.Identifier>("Expected type name")
        val type: DrType = when (token.value) {
            "Int"       -> IntType
            "String"    -> StringType
            "Bool"      -> BoolType
            "Null"      -> NullType
            "Void"      -> VoidType
            "Any"       -> AnyType
            "Last"      -> LastType
            else        -> ClassType(token.value)
        }

        advance()

        val isOptional = matchSymbol("?")
        val left: DrType =
            if (isOptional) OptionalType(type)
            else type

        if (isOptional && checkSymbol("|")) {
            throw DriftParserException("Cannot use both '?' and '|' in the same type declaration")
        }

        val unionTypes: MutableList<DrType> = mutableListOf(left)

        while (matchSymbol("|")) {
            val next = parseType()
            unionTypes.add(next)
        }

        return if (unionTypes.size == 1) {
            unionTypes[0]
        } else {
            UnionType(unionTypes)
        }
    }

    private fun parseReturn() : DrStmt =
        Return(parseExpression())


    private fun expectSymbol(expected: String) {
        val token = current()

        if (token !is Token.Symbol || token.value != expected) {
            throw DriftParserException("Expected '$expected' but found $token")
        }

        advance()
    }

    private fun checkSymbol(value: String) : Boolean {
        val token = current()

        return token is Token.Symbol && token.value == value
    }

    private fun matchSymbol(value: String) : Boolean {
        val token = current()

        if (token is Token.Symbol && token.value == value) {
            advance()

            return true
        }

        return false
    }

    private fun peekSymbol(value: String) : Boolean {
        val next = tokens.getOrNull(i + 1)

        return next is Token.Symbol && next.value == value
    }

    private inline fun <reified T : Token> expect(message: String) : T {
        val token = current()

        return token as? T ?: throw DriftParserException("Expected '$message' but found $token")
    }

    private fun isLambda() : Boolean {
        if (!checkSymbol("("))
            return false

        var j = i + 1
        var depth = 1

        while (j < tokens.size) {
            val token: Token? = tokens.getOrNull(j)

            if (token is Token.Symbol) {
                when (token.value) {
                    "(" -> depth++
                    ")" -> {
                        depth--

                        if (depth == 0)
                            j++
                            break
                    }
                }
            }

            j++
        }

        // Si on a un type de retour
        if (tokens.getOrNull(j)?.let { it is Token.Symbol && it.value == ":" } == true) {
            j++ // Skip ':'

            // Accepter des types comme Int | String? ou Int?
            while (j < tokens.size) {
                val t = tokens[j]

                val isTypePart = when (t) {
                    is Token.Identifier -> true
                    is Token.Symbol -> t.value in listOf("?", "|")
                    else -> false
                }

                if (!isTypePart) break
                j++
            }
        }

        val hasArrow = (tokens[j] as? Token.Symbol)?.value == "->"
        val hasBrace = (tokens[j+1] as? Token.Symbol)?.value == "{"

        return tokens.getOrNull(j) is Token.Symbol && hasArrow && hasBrace
    }
}