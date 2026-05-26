/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import drift.analysis.symbols.SymbolTable
import drift.ast.bindings.ForVariable
import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.metadata.Annotation
import drift.ast.statements.*
import drift.hir.metadata.HIRAnnotation
import drift.runtime.*
import drift.runtime.values.primaries.*
import drift.runtime.values.specials.ParserNull
import kotlin.test.*


class HIRConverterTest {

    private fun createConverter(
        ast: List<ParserStatement>,
        symbolTable: SymbolTable = SymbolTable(),
        typeResolution: Map<Int, ParserType> = emptyMap(),
        lambdaClosures: Map<Int, Map<String, Int>> = emptyMap()) : HIRConverter {

        HIRConverter.resetIds()

        return HIRConverter(ast, symbolTable, typeResolution, lambdaClosures)
    }

    // ========================================================================
    // LITERAL TESTS
    // ========================================================================

    @Test
    fun `convert integer literal`() {
        val literal = Literal(ParserInt(42))
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRExpressionStmt)
        val expr = (hir[0] as HIRExpressionStmt).expression
        assertTrue(expr is HIRLiteral)
        assertEquals(42, (expr as HIRLiteral).value)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), expr.type)
    }

    @Test
    fun `convert string literal`() {
        val literal = Literal(ParserString("hello"))
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to ObjectType("String"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val expr = (hir[0] as HIRExpressionStmt).expression as HIRLiteral
        assertEquals("hello", expr.value)
        assertEquals(HIRPrimitiveType(PrimitiveKind.STRING), expr.type)
    }

    @Test
    fun `convert boolean literal`() {
        val literal = Literal(ParserBool(true))
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to ObjectType("Bool"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val expr = (hir[0] as HIRExpressionStmt).expression as HIRLiteral
        assertEquals(true, expr.value)
        assertEquals(HIRPrimitiveType(PrimitiveKind.BOOL), expr.type)
    }

    @Test
    fun `convert null literal`() {
        val literal = Literal(ParserNull)
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to NullType)

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val expr = (hir[0] as HIRExpressionStmt).expression as HIRLiteral
        assertNull(expr.value)
        assertEquals(HIRPrimitiveType(PrimitiveKind.NULL), expr.type)
    }

    // ========================================================================
    // VARIABLE & LET TESTS
    // ========================================================================

    @Test
    fun `convert let statement with int`() {
        val initialValue = Literal(ParserInt(10))
        val let = Let(
            name = "x",
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(initialValue.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRVariable)
        val hirVar = hir[0] as HIRVariable
        assertEquals("x", hirVar.name)
        assertEquals(false, hirVar.isMutable)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), hirVar.type)
        assertTrue(hirVar.initialValue is HIRLiteral)
    }

    @Test
    fun `convert variable reference`() {
        // First define a variable
        val initialValue = Literal(ParserInt(5))
        val let = Let(
            name = "y",
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = true)

        // Then reference it
        val varRef = Variable("y")
        val exprStmt = ExprStmt(varRef)

        val ast = listOf(let, exprStmt)
        val typeResolution = mapOf(
            initialValue.nodeId to ObjectType("Int"),
            varRef.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        assertEquals(2, hir.size)
        assertTrue(hir[0] is HIRVariable)

        val exprHir = hir[1] as HIRExpressionStmt
        assertTrue(exprHir.expression is HIRVariableRef)
        val varRefHir = exprHir.expression as HIRVariableRef
        assertEquals("y", varRefHir.name)
    }

    // ========================================================================
    // BINARY OPERATION TESTS
    // ========================================================================

    @Test
    fun `convert addition operation`() {
        val left = Literal(ParserInt(3))
        val right = Literal(ParserInt(4))
        val binary = Binary(left, "+", right)
        val exprStmt = ExprStmt(binary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            left.nodeId to ObjectType("Int"),
            right.nodeId to ObjectType("Int"),
            binary.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val exprHir = (hir[0] as HIRExpressionStmt).expression
        assertTrue(exprHir is HIRBinaryOp)
        val binOp = exprHir as HIRBinaryOp
        assertEquals(BinaryOperator.ADD, binOp.operator)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), binOp.type)
    }

    @Test
    fun `convert subtraction operation`() {
        val left = Literal(ParserInt(10))
        val right = Literal(ParserInt(2))
        val binary = Binary(left, "-", right)
        val exprStmt = ExprStmt(binary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            left.nodeId to ObjectType("Int"),
            right.nodeId to ObjectType("Int"),
            binary.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val binOp = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        assertEquals(BinaryOperator.SUB, binOp.operator)
    }

    @Test
    fun `convert comparison operation`() {
        val left = Literal(ParserInt(5))
        val right = Literal(ParserInt(3))
        val binary = Binary(left, ">", right)
        val exprStmt = ExprStmt(binary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            left.nodeId to ObjectType("Int"),
            right.nodeId to ObjectType("Int"),
            binary.nodeId to ObjectType("Bool")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val binOp = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        assertEquals(BinaryOperator.GT, binOp.operator)
        assertEquals(HIRPrimitiveType(PrimitiveKind.BOOL), binOp.type)
    }

    @Test
    fun `convert logical AND operation`() {
        val left = Literal(ParserBool(true))
        val right = Literal(ParserBool(false))
        val binary = Binary(left, "&&", right)
        val exprStmt = ExprStmt(binary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            left.nodeId to ObjectType("Bool"),
            right.nodeId to ObjectType("Bool"),
            binary.nodeId to ObjectType("Bool")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val binOp = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        assertEquals(BinaryOperator.AND, binOp.operator)
    }

    // ========================================================================
    // UNARY OPERATION TESTS
    // ========================================================================

    @Test
    fun `convert unary negation`() {
        val operand = Literal(ParserInt(7))
        val unary = Unary("-", operand)
        val exprStmt = ExprStmt(unary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            operand.nodeId to ObjectType("Int"),
            unary.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val unaryOp = (hir[0] as HIRExpressionStmt).expression as HIRUnaryOp
        assertEquals(UnaryOperator.NEGATE, unaryOp.operator)
    }

    @Test
    fun `convert logical NOT`() {
        val operand = Literal(ParserBool(true))
        val unary = Unary("!", operand)
        val exprStmt = ExprStmt(unary)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            operand.nodeId to ObjectType("Bool"),
            unary.nodeId to ObjectType("Bool")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val unaryOp = (hir[0] as HIRExpressionStmt).expression as HIRUnaryOp
        assertEquals(UnaryOperator.NOT, unaryOp.operator)
    }

    // ========================================================================
    // FUNCTION TESTS
    // ========================================================================

    @Test
    fun `convert simple function`() {
        val returnExpr = Literal(ParserInt(42))
        val function = Func(
            name = "getAnswer",
            parameters = emptyList(),
            body = Block(listOf(ExprStmt(returnExpr))),
            returnType = ObjectType("Int")
        )
        val ast = listOf(function)
        val typeResolution = mapOf(returnExpr.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRFunction)
        val hirFunc = hir[0] as HIRFunction
        assertEquals("getAnswer", hirFunc.name)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), hirFunc.returnType)
        assertEquals(0, hirFunc.parameters.size)
    }

    @Test
    fun `convert function with parameters`() {
        val param1 = FunctionParameter("a", isPositional = true, type = ObjectType("Int"))
        val param2 = FunctionParameter("b", isPositional = true, type = ObjectType("Int"))
        val returnExpr = Binary(Variable("a"), "+", Variable("b"))

        val function = Func(
            name = "add",
            parameters = listOf(param1, param2),
            body = Block(listOf(ExprStmt(returnExpr))),
            returnType = ObjectType("Int")
        )
        val ast = listOf(function)
        val typeResolution = mapOf(returnExpr.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirFunc = hir[0] as HIRFunction
        assertEquals("add", hirFunc.name)
        assertEquals(2, hirFunc.parameters.size)
        assertEquals("a", hirFunc.parameters[0].name)
        assertEquals("b", hirFunc.parameters[1].name)
    }

    // ========================================================================
    // CONTROL FLOW TESTS
    // ========================================================================

    @Test
    fun `convert if statement`() {
        val condition = Literal(ParserBool(true))
        val thenBranch = ExprStmt(Literal(ParserInt(1)))
        val ifStmt = If(condition, thenBranch, null)
        val ast = listOf(ifStmt)
        val typeResolution = mapOf(condition.nodeId to ObjectType("Bool"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRExpressionStmt)
        val conditional = (hir[0] as HIRExpressionStmt).expression as HIRConditional
        assertEquals(HIRPrimitiveType(PrimitiveKind.VOID), conditional.type)
        assertNull(conditional.elseBranch)
    }

    @Test
    fun `convert if-else statement`() {
        val condition = Literal(ParserBool(true))
        val thenBranch = ExprStmt(Literal(ParserInt(1)))
        val elseBranch = ExprStmt(Literal(ParserInt(0)))
        val ifStmt = If(condition, thenBranch, elseBranch)
        val ast = listOf(ifStmt)
        val typeResolution = mapOf(condition.nodeId to ObjectType("Bool"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val conditional = (hir[0] as HIRExpressionStmt).expression as HIRConditional
        assertNotNull(conditional.elseBranch)
    }

    @Test
    fun `convert block statement`() {
        val let1 = Let(
            name ="x",
            type = ObjectType("Int"),
            value = Literal(ParserInt(1)),
            isMutable = false)

        val let2 = Let(
            name ="y",
            type = ObjectType("Int"),
            value = Literal(ParserInt(2)),
            isMutable = false)

        val block = Block(listOf(let1, let2))
        val ast = listOf(block)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        assertTrue(hir[0] is HIRBlock)
        val hirBlock = hir[0] as HIRBlock
        assertEquals(2, hirBlock.statements.size)
        assertTrue(hirBlock.statements[0] is HIRVariable)
        assertTrue(hirBlock.statements[1] is HIRVariable)
    }

    // ========================================================================
    // TYPE CONVERSION TESTS
    // ========================================================================

    @Test
    fun `convert int type`() {
        val typeResolution = mapOf(0 to ObjectType("Int"))
        val result = convertParserTypeToHIRType(ObjectType("Int"))
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), result)
    }

    @Test
    fun `convert string type`() {
        val result = convertParserTypeToHIRType(ObjectType("String"))
        assertEquals(HIRPrimitiveType(PrimitiveKind.STRING), result)
    }

    @Test
    fun `convert bool type`() {
        val result = convertParserTypeToHIRType(ObjectType("Bool"))
        assertEquals(HIRPrimitiveType(PrimitiveKind.BOOL), result)
    }

    @Test
    fun `convert optional type`() {
        val result = convertParserTypeToHIRType(OptionalType(ObjectType("Int")))
        assertTrue(result is HIROptionalType)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), (result as HIROptionalType).innerType)
    }

    @Test
    fun `convert union type`() {
        val result = convertParserTypeToHIRType(UnionType(listOf(ObjectType("Int"), ObjectType("String"))))
        assertTrue(result is HIRUnionType)
    }

    // ========================================================================
    // ASSIGNMENT TESTS
    // ========================================================================

    @Test
    fun `convert variable assignment`() {
        val assign = Assign("x", Literal(ParserInt(99)))
        val exprStmt = ExprStmt(assign)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(assign.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val assignHir = (hir[0] as HIRExpressionStmt).expression as HIRAssign
        assertTrue(assignHir.target is VariableTarget)
        assertEquals("x", (assignHir.target as VariableTarget).name)
    }

    // ========================================================================
    // COMPLEX EXPRESSION TESTS
    // ========================================================================

    @Test
    fun `convert nested binary operations`() {
        val a = Literal(ParserInt(1))
        val b = Literal(ParserInt(2))
        val c = Literal(ParserInt(3))
        val add = Binary(a, "+", b)
        val mul = Binary(add, "*", c)
        val exprStmt = ExprStmt(mul)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            a.nodeId to ObjectType("Int"),
            b.nodeId to ObjectType("Int"),
            c.nodeId to ObjectType("Int"),
            add.nodeId to ObjectType("Int"),
            mul.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val mulHir = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        assertEquals(BinaryOperator.MUL, mulHir.operator)
        assertTrue(mulHir.left is HIRBinaryOp)
    }

    @Test
    fun `convert multiple statements`() {
        val let1 = Let(
            name = "a",
            type = ObjectType("Int"),
            value = Literal(ParserInt(10)),
            isMutable = false)

        val let2 = Let(
            name = "b",
            type = ObjectType("String"),
            value = Literal(ParserString("hi")),
            isMutable = false)

        val let3 = Let(
            name = "c",
            type = ObjectType("Bool"),
            value = Literal(ParserBool(true)),
            isMutable = false)

        val ast = listOf(let1, let2, let3)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        assertEquals(3, hir.size)
        assertTrue(hir.all { it is HIRVariable })
    }

    // ========================================================================
    // CLASS DECLARATION TESTS
    // ========================================================================

    @Test
    fun `convert simple class`() {
        val field1 = Let(
            name = "id",
            type = ObjectType("Int"),
            value = Literal(ParserInt(0)),
            isMutable = false)

        val field2 = Let(
            name = "name",
            type = ObjectType("String"),
            value = Literal(ParserString("")),
            isMutable = false)

        val klass = Class(
            name = "User",
            fields = mutableListOf(field1, field2),
            methods = mutableListOf(),
            staticFields = mutableListOf(),
            staticMethods = mutableListOf(),
            hasPrimaryConstructor = false
        )

        val ast = listOf(klass)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        assertTrue(hir[0] is HIRClass)
        val hirClass = hir[0] as HIRClass
        assertEquals("User", hirClass.name)
        assertEquals(2, hirClass.fields.size)
    }

    @Test
    fun `convert class with methods`() {
        val method = Func(
            name = "getName",
            parameters = emptyList(),
            body = Block(listOf(ExprStmt(Literal(ParserString("test"))))),
            returnType = ObjectType("String")
        )
        val klass = Class(
            name = "Person",
            fields = mutableListOf(),
            methods = mutableListOf(method),
            staticFields = mutableListOf(),
            staticMethods = mutableListOf(),
            hasPrimaryConstructor = false
        )
        val ast = listOf(klass)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirClass = hir[0] as HIRClass
        assertEquals(1, hirClass.methods.size)
        assertEquals("getName", hirClass.methods[0].name)
    }

    @Test
    fun `convert class with static fields and methods`() {
        val staticField = Let(
            name = "count",
            type = ObjectType("Int"),
            value = Literal(ParserInt(0)),
            isMutable = false)

        val staticMethod = Func(
            name = "getCount",
            parameters = emptyList(),
            body = Block(listOf(ExprStmt(Literal(ParserInt(0))))),
            returnType = ObjectType("Int")
        )
        val klass = Class(
            name = "Counter",
            fields = mutableListOf(),
            methods = mutableListOf(),
            staticFields = mutableListOf(staticField),
            staticMethods = mutableListOf(staticMethod),
            hasPrimaryConstructor = false
        )
        val ast = listOf(klass)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirClass = hir[0] as HIRClass
        assertEquals(1, hirClass.staticFields.size)
        assertEquals(1, hirClass.staticMethods.size)
    }

    // ========================================================================
    // FOR LOOP TESTS
    // ========================================================================

    @Test
    fun `convert simple for loop`() {
        val iterable = Variable("items")
        val forVar = ForVariable("item")
        val body = Block(listOf(ExprStmt(Variable("item"))))
        val forLoop = For(iterable, listOf(forVar), body)
        val ast = listOf(forLoop)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        assertTrue(hir[0] is HIRExpressionStmt)
        val loopExpr = (hir[0] as HIRExpressionStmt).expression as HIRLoop
        assertEquals("item", loopExpr.iteratorVariable)
    }

    @Test
    fun `convert for loop with range`() {
        val range = Binary(Literal(ParserInt(0)), "..", Literal(ParserInt(10)))
        val forVar = ForVariable("i")
        val body = Block(listOf(ExprStmt(Variable("i"))))
        val forLoop = For(range, listOf(forVar), body)
        val ast = listOf(forLoop)
        val typeResolution = mapOf(
            range.nodeId to ObjectType("InclusiveRange")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val loopExpr = (hir[0] as HIRExpressionStmt).expression as HIRLoop
        assertEquals("i", loopExpr.iteratorVariable)
        assertTrue(loopExpr.iterable is HIRBinaryOp)
    }

    // ========================================================================
    // LAMBDA TESTS
    // ========================================================================

    @Test
    fun `convert simple lambda`() {
        val body = Block(listOf(ExprStmt(Literal(ParserInt(42)))))
        val lambda = Lambda(
            parameters = emptyList(),
            body = body,
            returnType = ObjectType("Int")
        )
        val exprStmt = ExprStmt(lambda)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            lambda.nodeId to ObjectType("Function")
        )
        val lambdaClosures = mapOf(lambda.nodeId to emptyMap<String, Int>())

        val converter = createConverter(ast, typeResolution = typeResolution, lambdaClosures = lambdaClosures)
        val hir = converter.convert()

        val lambdaExpr = (hir[0] as HIRExpressionStmt).expression as HIRLambda
        assertEquals(0, lambdaExpr.parameters.size)
        assertEquals(0, lambdaExpr.capturedVariables.size)
    }

    @Test
    fun `convert lambda with parameters`() {
        val param = FunctionParameter("x", isPositional = true, type = ObjectType("Int"))
        val body = Block(listOf(ExprStmt(Variable("x"))))
        val lambda = Lambda(
            parameters = listOf(param),
            body = body,
            returnType = ObjectType("Int")
        )
        val exprStmt = ExprStmt(lambda)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            lambda.nodeId to ObjectType("Function")
        )
        val lambdaClosures = mapOf(lambda.nodeId to emptyMap<String, Int>())

        val converter = createConverter(ast, typeResolution = typeResolution, lambdaClosures = lambdaClosures)
        val hir = converter.convert()

        val lambdaExpr = (hir[0] as HIRExpressionStmt).expression as HIRLambda
        assertEquals(1, lambdaExpr.parameters.size)
        assertEquals("x", lambdaExpr.parameters[0].name)
    }

    @Test
    fun `convert lambda with captured variables`() {
        val letStmt = Let(
            name = "y",
            type = ObjectType("Int"),
            value = Literal(ParserInt(5)),
            isMutable = false)

        val body = Block(listOf(ExprStmt(Variable("y"))))
        val lambda = Lambda(
            parameters = emptyList(),
            body = body,
            returnType = ObjectType("Int")
        )
        val exprStmt = ExprStmt(lambda)
        val ast = listOf(letStmt, exprStmt)
        val typeResolution = mapOf(
            lambda.nodeId to ObjectType("Function"),
            letStmt.value.nodeId to ObjectType("Int")
        )
        val lambdaClosures = mapOf(lambda.nodeId to mapOf("y" to letStmt.nodeId))

        val converter = createConverter(ast, typeResolution = typeResolution, lambdaClosures = lambdaClosures)
        val hir = converter.convert()

        val lambdaExpr = (hir[1] as HIRExpressionStmt).expression as HIRLambda
        assertEquals(1, lambdaExpr.capturedVariables.size)
        assertEquals("y", lambdaExpr.capturedVariables[0].name)
    }

    // ========================================================================
    // CALL EXPRESSION TESTS
    // ========================================================================

    @Test
    fun `convert function call without arguments`() {
        val callee = Variable("greet")
        val call = Call(callee, emptyList())
        val exprStmt = ExprStmt(call)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            callee.nodeId to ObjectType("Function"),
            call.nodeId to ObjectType("String")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val callExpr = (hir[0] as HIRExpressionStmt).expression as HIRCall
        assertEquals(0, callExpr.arguments.size)
    }

    @Test
    fun `convert function call with arguments`() {
        val callee = Variable("add")
        val arg1 = Argument(null, Literal(ParserInt(1)))
        val arg2 = Argument(null, Literal(ParserInt(2)))
        val call = Call(callee, listOf(arg1, arg2))
        val exprStmt = ExprStmt(call)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            callee.nodeId to ObjectType("Function"),
            arg1.expr.nodeId to ObjectType("Int"),
            arg2.expr.nodeId to ObjectType("Int"),
            call.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val callExpr = (hir[0] as HIRExpressionStmt).expression as HIRCall
        assertEquals(2, callExpr.arguments.size)
    }

    @Test
    fun `convert function call with named arguments`() {
        val callee = Variable("create")
        val arg1 = Argument("name", Literal(ParserString("Alice")))
        val arg2 = Argument("age", Literal(ParserInt(30)))
        val call = Call(callee, listOf(arg1, arg2))
        val exprStmt = ExprStmt(call)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            callee.nodeId to ObjectType("Function"),
            arg1.expr.nodeId to ObjectType("String"),
            arg2.expr.nodeId to ObjectType("Int"),
            call.nodeId to ObjectType("User")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val callExpr = (hir[0] as HIRExpressionStmt).expression as HIRCall
        assertEquals("name", callExpr.arguments[0].name)
        assertEquals("age", callExpr.arguments[1].name)
    }

    // ========================================================================
    // GET/SET TESTS
    // ========================================================================

    @Test
    fun `convert field get expression`() {
        val receiver = Variable("user")
        val get = Get(receiver, "name")
        val exprStmt = ExprStmt(get)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            receiver.nodeId to ObjectType("User"),
            get.nodeId to ObjectType("String")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val fieldAccess = (hir[0] as HIRExpressionStmt).expression as HIRFieldAccess
        assertEquals("name", fieldAccess.fieldName)
        assertEquals("User", fieldAccess.receiverClassName)
    }

    @Test
    fun `convert field set expression`() {
        val receiver = Variable("user")
        val set = Set(receiver, "name", Literal(ParserString("Bob")))
        val exprStmt = ExprStmt(set)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            receiver.nodeId to ObjectType("User"),
            set.nodeId to ObjectType("String")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val assign = (hir[0] as HIRExpressionStmt).expression as HIRAssign
        assertTrue(assign.target is FieldTarget)
        assertEquals("name", (assign.target as FieldTarget).fieldName)
    }

    // ========================================================================
    // CONDITIONAL EXPRESSION TESTS
    // ========================================================================

    @Test
    fun `convert ternary conditional expression`() {
        val condition = Literal(ParserBool(true))
        val thenExpr = Literal(ParserInt(1))
        val elseExpr = Literal(ParserInt(0))
        val conditional = Conditional(
            condition = condition,
            thenBranch = ExprStmt(thenExpr),
            elseBranch = ExprStmt(elseExpr)
        )
        val exprStmt = ExprStmt(conditional)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            condition.nodeId to ObjectType("Bool"),
            thenExpr.nodeId to ObjectType("Int"),
            elseExpr.nodeId to ObjectType("Int"),
            conditional.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val condExpr = (hir[0] as HIRExpressionStmt).expression as HIRConditional
        assertNotNull(condExpr.elseBranch)
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT), condExpr.type)
    }

    @Test
    fun `convert conditional without else`() {
        val condition = Literal(ParserBool(false))
        val thenExpr = Literal(ParserInt(5))
        val conditional = Conditional(
            condition = condition,
            thenBranch = ExprStmt(thenExpr),
            elseBranch = null
        )
        val exprStmt = ExprStmt(conditional)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            condition.nodeId to ObjectType("Bool"),
            thenExpr.nodeId to ObjectType("Int"),
            conditional.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val condExpr = (hir[0] as HIRExpressionStmt).expression as HIRConditional
        assertNull(condExpr.elseBranch)
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    fun `convert function with empty body`() {
        val function = Func(
            name = "noop",
            parameters = emptyList(),
            body = Block.empty(),
            returnType = ObjectType("Void")
        )
        val ast = listOf(function)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirFunc = hir[0] as HIRFunction
        assertEquals(0, hirFunc.body.size)
    }

    @Test
    fun `convert multiple classes`() {
        val class1 = Class(
            name = "User",
            fields = mutableListOf(),
            staticFields = mutableListOf(),
            methods = mutableListOf(),
            staticMethods =  mutableListOf(),
            hasPrimaryConstructor = false)

        val class2 = Class(
            name = "Admin",
            fields = mutableListOf(),
            staticFields = mutableListOf(),
            methods = mutableListOf(),
            staticMethods =  mutableListOf(),
            hasPrimaryConstructor = false)

        val ast = listOf(class1, class2)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        assertEquals(2, hir.size)
        assertTrue(hir.all { it is HIRClass })
    }

    @Test
    fun `convert deeply nested expressions`() {
        val a = Literal(ParserInt(1))
        val b = Literal(ParserInt(2))
        val c = Literal(ParserInt(3))
        val d = Literal(ParserInt(4))

        val add1 = Binary(a, "+", b)
        val mul1 = Binary(add1, "*", c)
        val sub1 = Binary(mul1, "-", d)

        val exprStmt = ExprStmt(sub1)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(
            a.nodeId to ObjectType("Int"),
            b.nodeId to ObjectType("Int"),
            c.nodeId to ObjectType("Int"),
            d.nodeId to ObjectType("Int"),
            add1.nodeId to ObjectType("Int"),
            mul1.nodeId to ObjectType("Int"),
            sub1.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val subExpr = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        assertEquals(BinaryOperator.SUB, subExpr.operator)
        assertTrue(subExpr.left is HIRBinaryOp)
    }

    @Test
    fun `convert int64 type`() {
        val literal = Literal(ParserInt64(999999999L))
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to ObjectType("Int64"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val lit = (hir[0] as HIRExpressionStmt).expression as HIRLiteral
        assertEquals(HIRPrimitiveType(PrimitiveKind.INT64), lit.type)
    }

    @Test
    fun `convert uint type`() {
        val literal = Literal(ParserUInt(42u))
        val exprStmt = ExprStmt(literal)
        val ast = listOf(exprStmt)
        val typeResolution = mapOf(literal.nodeId to ObjectType("UInt"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val lit = (hir[0] as HIRExpressionStmt).expression as HIRLiteral
        assertEquals(HIRPrimitiveType(PrimitiveKind.UINT), lit.type)
    }

    @Test
    fun `convert range operators`() {
        val left = Literal(ParserInt(1))
        val right = Literal(ParserInt(10))

        val inclusiveRange = Binary(left, "..", right)
        val exclRange = Binary(left, "..<", right)

        val ast = listOf(
            ExprStmt(inclusiveRange),
            ExprStmt(exclRange)
        )
        val typeResolution = mapOf(
            left.nodeId to ObjectType("Int"),
            right.nodeId to ObjectType("Int"),
            inclusiveRange.nodeId to ObjectType("InclusiveRange"),
            exclRange.nodeId to ObjectType("ExclusiveRange")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val incRange = (hir[0] as HIRExpressionStmt).expression as HIRBinaryOp
        val excRange = (hir[1] as HIRExpressionStmt).expression as HIRBinaryOp

        assertEquals(BinaryOperator.INCLUSIVE_RANGE, incRange.operator)
        assertEquals(BinaryOperator.EXCLUSIVE_RANGE, excRange.operator)
    }

    // ========================================================================
    // IMPORT STATEMENT TESTS
    // ========================================================================

    @Test
    fun `convert simple import statement`() {
        val import = Import(
            namespace = "module",
            steps = listOf("module"),
            alias = null,
            parts = null,
            wildcard = false
        )
        val ast = listOf(import)

        val converter = createConverter(ast)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRImport)
        val hirImport = hir[0] as HIRImport
        assertEquals("module", hirImport.namespace)
        assertEquals(listOf("module"), hirImport.steps)
        assertNull(hirImport.alias)
        assertNull(hirImport.parts)
        assertFalse(hirImport.wildcard)
    }

    @Test
    fun `convert import with alias`() {
        val import = Import(
            namespace = "drift.math",
            steps = listOf("drift", "math"),
            alias = "m",
            parts = null,
            wildcard = false
        )
        val ast = listOf(import)

        val converter = createConverter(ast)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRImport)
        val hirImport = hir[0] as HIRImport
        assertEquals("drift.math", hirImport.namespace)
        assertEquals(listOf("drift", "math"), hirImport.steps)
        assertEquals("m", hirImport.alias)
    }

    @Test
    fun `convert wildcard import`() {
        val import = Import(
            namespace = "drift.utils",
            steps = listOf("drift", "utils"),
            alias = null,
            parts = null,
            wildcard = true
        )
        val ast = listOf(import)

        val converter = createConverter(ast)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRImport)
        val hirImport = hir[0] as HIRImport
        assertTrue(hirImport.wildcard)
    }

    @Test
    fun `convert selective import with parts`() {
        val parts = listOf(
            ImportPart(source = "MyClass", alias = null),
            ImportPart(source = "MyFunction", alias = "fn")
        )
        val import = Import(
            namespace = "mymodule",
            steps = listOf("mymodule"),
            alias = null,
            parts = parts,
            wildcard = false
        )
        val ast = listOf(import)

        val converter = createConverter(ast)
        val hir = converter.convert()

        assertTrue(hir[0] is HIRImport)
        val hirImport = hir[0] as HIRImport
        assertNotNull(hirImport.parts)
        assertEquals(2, hirImport.parts!!.size)
        assertEquals("MyClass", hirImport.parts!![0].source)
        assertNull(hirImport.parts!![0].alias)
        assertEquals("MyFunction", hirImport.parts!![1].source)
        assertEquals("fn", hirImport.parts!![1].alias)
    }

    // ========================================================================
    // ANNOTATION TESTS
    // ========================================================================

    @Test
    fun `convert let with single annotation no args`() {
        val annotation = Annotation(name = "Deprecated", args = emptyList())
        val initialValue = Literal(ParserInt(0))
        val let = Let(
            name = "x",
            annotations = mutableListOf(annotation),
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(initialValue.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirVar = hir[0] as HIRVariable
        assertEquals(1, hirVar.annotations.size)
        assertEquals("Deprecated", hirVar.annotations[0].name)
        assertEquals(0, hirVar.annotations[0].args.size)
    }

    @Test
    fun `convert let with annotation with positional arg`() {
        val argExpr = Literal(ParserString("use newFn instead"))
        val annotation = Annotation(
            name = "Deprecated",
            args = listOf(Argument(null, argExpr))
        )
        val initialValue = Literal(ParserInt(0))
        val let = Let(
            name = "x",
            annotations = mutableListOf(annotation),
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(
            initialValue.nodeId to ObjectType("Int"),
            argExpr.nodeId to ObjectType("String")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirVar = hir[0] as HIRVariable
        assertEquals(1, hirVar.annotations.size)
        val hirAnnotation = hirVar.annotations[0]
        assertEquals("Deprecated", hirAnnotation.name)
        assertEquals(1, hirAnnotation.args.size)
        assertNull(hirAnnotation.args[0].name)
        assertEquals("use newFn instead", (hirAnnotation.args[0].value as HIRLiteral).value)
    }

    @Test
    fun `convert let with annotation with named arg`() {
        val argExpr = Literal(ParserString("reason"))
        val annotation = Annotation(
            name = "Suppress",
            args = listOf(Argument("message", argExpr))
        )
        val initialValue = Literal(ParserInt(1))
        val let = Let(
            name = "y",
            annotations = mutableListOf(annotation),
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(
            initialValue.nodeId to ObjectType("Int"),
            argExpr.nodeId to ObjectType("String")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirVar = hir[0] as HIRVariable
        val hirAnnotation = hirVar.annotations[0]
        assertEquals("message", hirAnnotation.args[0].name)
    }

    @Test
    fun `convert let with multiple annotations`() {
        val ann1 = Annotation(name = "Deprecated", args = emptyList())
        val ann2 = Annotation(name = "Internal", args = emptyList())
        val initialValue = Literal(ParserInt(0))
        val let = Let(
            name = "z",
            annotations = mutableListOf(ann1, ann2),
            type = ObjectType("Int"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(initialValue.nodeId to ObjectType("Int"))

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirVar = hir[0] as HIRVariable
        assertEquals(2, hirVar.annotations.size)
        assertEquals("Deprecated", hirVar.annotations[0].name)
        assertEquals("Internal", hirVar.annotations[1].name)
    }

    @Test
    fun `convert function with annotation`() {
        val annotation = Annotation(name = "Override", args = emptyList())
        val function = Func(
            name = "compute",
            annotations = mutableListOf(annotation),
            parameters = emptyList(),
            body = Block.empty(),
            returnType = ObjectType("Void")
        )
        val ast = listOf(function)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirFunc = hir[0] as HIRFunction
        assertEquals(1, hirFunc.annotations.size)
        assertEquals("Override", hirFunc.annotations[0].name)
    }

    @Test
    fun `convert function with no annotations produces empty list`() {
        val function = Func(
            name = "noop",
            parameters = emptyList(),
            body = Block.empty(),
            returnType = ObjectType("Void")
        )
        val ast = listOf(function)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirFunc = hir[0] as HIRFunction
        assertEquals(0, hirFunc.annotations.size)
    }

    @Test
    fun `convert class with annotation`() {
        val annotation = Annotation(name = "Serializable", args = emptyList())
        val klass = Class(
            name = "Config",
            annotations = mutableListOf(annotation),
            fields = mutableListOf(),
            methods = mutableListOf(),
            staticFields = mutableListOf(),
            staticMethods = mutableListOf(),
            hasPrimaryConstructor = false
        )
        val ast = listOf(klass)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirClass = hir[0] as HIRClass
        assertEquals(1, hirClass.annotations.size)
        assertEquals("Serializable", hirClass.annotations[0].name)
    }

    @Test
    fun `convert class field with annotation`() {
        val annotation = Annotation(name = "Transient", args = emptyList())
        val field = Let(
            name = "password",
            annotations = mutableListOf(annotation),
            type = ObjectType("String"),
            value = Literal(ParserString("")),
            isMutable = false
        )
        val klass = Class(
            name = "User",
            fields = mutableListOf(field),
            methods = mutableListOf(),
            staticFields = mutableListOf(),
            staticMethods = mutableListOf(),
            hasPrimaryConstructor = false
        )
        val ast = listOf(klass)

        val converter = createConverter(ast, typeResolution = emptyMap())
        val hir = converter.convert()

        val hirClass = hir[0] as HIRClass
        assertEquals(1, hirClass.fields.size)
        val hirField = hirClass.fields[0]
        assertEquals(1, hirField.annotations.size)
        assertEquals("Transient", hirField.annotations[0].name)
    }

    @Test
    fun `convert annotation with multiple args`() {
        val arg1 = Literal(ParserString("message"))
        val arg2 = Literal(ParserInt(42))
        val annotation = Annotation(
            name = "Meta",
            args = listOf(
                Argument("label", arg1),
                Argument("code", arg2)
            )
        )
        val initialValue = Literal(ParserBool(true))
        val let = Let(
            name = "flag",
            annotations = mutableListOf(annotation),
            type = ObjectType("Bool"),
            value = initialValue,
            isMutable = false
        )
        val ast = listOf(let)
        val typeResolution = mapOf(
            initialValue.nodeId to ObjectType("Bool"),
            arg1.nodeId to ObjectType("String"),
            arg2.nodeId to ObjectType("Int")
        )

        val converter = createConverter(ast, typeResolution = typeResolution)
        val hir = converter.convert()

        val hirVar = hir[0] as HIRVariable
        val hirAnnotation = hirVar.annotations[0]
        assertEquals(2, hirAnnotation.args.size)
        assertEquals("label", hirAnnotation.args[0].name)
        assertEquals("code", hirAnnotation.args[1].name)
        assertEquals("message", (hirAnnotation.args[0].value as HIRLiteral).value)
        assertEquals(42, (hirAnnotation.args[1].value as HIRLiteral).value)
    }
}