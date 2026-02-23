/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.hir.*
import drift.runtime.ParserType
import drift.runtime.AnyType
import drift.runtime.ObjectType
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.primaries.ParserUInt
import drift.runtime.values.specials.ParserNull

/**
 * Converter from Drift AST to HIR (High-level Intermediate Representation).
 */
class HIRConverter(
    private val ast: List<ParserStatement>,
    private val symbolTable: SymbolTable,
    private val typeResolution: Map<Int, ParserType>,
    private val lambdaClosures: Map<Int, Map<String, Int>>) {

    companion object {

        private var nextHirId = 0
        
        fun allocateHirId() : Int = nextHirId++
        
        fun resetIds() {
            nextHirId = 0
        }
    }


    private val astToHirMap = mutableMapOf<Int, Int>()
    private val definitionHirIds = mutableMapOf<String, Int>()


    fun convert() : List<HIRStatement> {
        return ast.map { convertStatement(it) }
    }


    // ========================================================================
    // STATEMENTS
    // ========================================================================

    private fun convertStatement(stmt: ParserStatement) : HIRStatement {
        return when (stmt) {
            is Let -> convertLet(stmt)
            is Func -> convertFunction(stmt)
            is Class -> convertClass(stmt)
            is If -> convertIfStmt(stmt)
            is For -> convertFor(stmt)
            is Return -> convertReturn(stmt)
            is Block -> convertBlock(stmt)
            is ExprStmt -> convertExprStmt(stmt)
            is Import -> convertImport(stmt)

            else -> error("Unknown statement type: ${stmt::class.simpleName}")
        }
    }

    private fun convertLet(let: Let) : HIRStatement {
        val hirId = allocateHirId()
        val type = convertType(typeResolution[let.nodeId] ?: let.type)
        val initialValue = convertExpression(let.value)

        val hirVar = HIRVariable(
            hirId = hirId,
            name = let.name,
            type = type,
            initialValue = initialValue,
            isMutable = let.isMutable)

        astToHirMap[let.nodeId] = hirId
        definitionHirIds[let.name] = hirId

        return hirVar
    }

    private fun convertFunction(function: Func) : HIRStatement {
        val hirId = allocateHirId()
        val returnType = convertType(function.returnType)
        
        val parameters = function.parameters.map { param ->
            HIRParameter(
                name = param.name,
                type = convertType(param.type),
                defaultValue = param.defaultValue?.let { convertExpression(it) })
        }

        val body = function.body.map { convertStatement(it) }

        val hirFunc = HIRFunction(
            hirId = hirId,
            name = function.name,
            parameters = parameters,
            returnType = returnType,
            body = body)

        astToHirMap[function.nodeId] = hirId
        definitionHirIds[function.name] = hirId

        return hirFunc
    }

    private fun convertClass(clazz: Class) : HIRStatement {
        val hirId = allocateHirId()

        val fields = clazz.fields.map { field ->
            HIRField(
                name = field.name,
                type = convertType(field.type),
                isStatic = false)
        }

        val methods = clazz.methods.map { convertFunction(it) as HIRFunction }

        val staticFields = clazz.staticFields.map { field ->
            HIRField(
                name = field.name,
                type = convertType(field.type),
                isStatic = true)
        }

        val staticMethods = clazz.staticMethods.map { convertFunction(it) as HIRFunction }

        val hirClass = HIRClass(
            hirId = hirId,
            name = clazz.name,
            fields = fields,
            methods = methods,
            staticFields = staticFields,
            staticMethods = staticMethods)

        astToHirMap[clazz.nodeId] = hirId
        definitionHirIds[clazz.name] = hirId

        return hirClass
    }

    private fun convertIfStmt(ifStmt: If) : HIRStatement {
        val hirId = allocateHirId()
        val condition = convertExpression(ifStmt.condition)
        val thenBranch = HIRBlock(allocateHirId(), listOf(convertStatement(ifStmt.thenBranch)))
        val elseBranch = ifStmt.elseBranch?.let { HIRBlock(allocateHirId(), listOf(convertStatement(it))) }

        val hirConditional = HIRConditional(
            hirId = hirId,
            type = HIRPrimitiveType(PrimitiveKind.VOID),
            condition = condition,
            thenBranch = thenBranch,
            elseBranch = elseBranch)

        astToHirMap[ifStmt.nodeId] = hirId
        return HIRExpressionStmt(allocateHirId(), hirConditional)
    }

    private fun convertFor(forLoop: For) : HIRStatement {
        val hirId = allocateHirId()
        val iterable = convertExpression(forLoop.iterable)

        val iteratorVariable = forLoop.variables.firstOrNull()?.name ?: "item"
        val iteratorType = HIRPrimitiveType(PrimitiveKind.INT)

        val body = HIRBlock(allocateHirId(), listOf(convertStatement(forLoop.body)))

        val hirLoop = HIRLoop(
            hirId = hirId,
            iteratorVariable = iteratorVariable,
            iteratorType = iteratorType,
            iterable = iterable,
            body = body)

        astToHirMap[forLoop.nodeId] = hirId
        return HIRExpressionStmt(allocateHirId(), hirLoop)
    }

    private fun convertReturn(returnStmt: Return) : HIRStatement {
        val hirId = allocateHirId()
        val value = convertExpression(returnStmt.value)

        val hirReturn = HIRReturn(
            hirId = hirId,
            value = value)

        astToHirMap[returnStmt.nodeId] = hirId
        return hirReturn
    }

    private fun convertBlock(block: Block) : HIRStatement {
        val hirId = allocateHirId()
        val statements = block.statements.map { convertStatement(it) }

        val hirBlock = HIRBlock(
            hirId = hirId,
            statements = statements)

        astToHirMap[block.nodeId] = hirId
        return hirBlock
    }

    private fun convertExprStmt(exprStmt: ExprStmt) : HIRStatement {
        val hirId = allocateHirId()
        val expression = convertExpression(exprStmt.expr)

        val hirExprStmt = HIRExpressionStmt(
            hirId = hirId,
            expression = expression)

        astToHirMap[exprStmt.nodeId] = hirId
        return hirExprStmt
    }

    private fun convertImport(importStmt: Import) : HIRStatement {
        val hirId = allocateHirId()

        val hirImportParts = importStmt.parts?.map { part ->
            HIRImportPart(
                source = part.source,
                alias = part.alias)
        }

        val hirImport = HIRImport(
            hirId = hirId,
            namespace = importStmt.namespace,
            steps = importStmt.steps,
            alias = importStmt.alias,
            parts = hirImportParts,
            wildcard = importStmt.wildcard)

        astToHirMap[importStmt.nodeId] = hirId

        return hirImport
    }


    // ========================================================================
    // EXPRESSIONS
    // ========================================================================

    private fun convertExpression(expr: ParserExpression) : HIRExpression {
        val type = convertType(typeResolution[expr.nodeId] ?: AnyType)

        return when (expr) {
            is Literal -> convertLiteral(expr, type)
            is Variable -> convertVariable(expr, type)
            is Binary -> convertBinary(expr, type)
            is Unary -> convertUnary(expr, type)
            is Call -> convertCall(expr, type)
            is Get -> convertGet(expr, type)
            is Set -> convertSet(expr, type)
            is Assign -> convertAssign(expr, type)
            is Conditional -> convertConditional(expr, type)
            is Lambda -> convertLambda(expr, type)
            is ListLiteral -> TODO("List literal handling in HIR")

            else -> error("Unknown expression type: ${expr::class.simpleName}")
        }
    }

    private fun convertLiteral(literal: Literal, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val value = when (val v = literal.value) {
            is ParserInt -> v.value
            is ParserInt64 -> v.value
            is ParserUInt -> v.value
            is ParserBool -> v.value
            is ParserString -> v.value
            is ParserNull -> null

            else -> null
        }

        val hirLiteral = HIRLiteral(
            hirId = hirId,
            type = type,
            value = value)

        astToHirMap[literal.nodeId] = hirId

        return hirLiteral
    }

    private fun convertVariable(variable: Variable, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val definitionHirId = definitionHirIds[variable.name] ?: -1

        val hirVar = HIRVariableRef(
            hirId = hirId,
            type = type,
            name = variable.name,
            definitionHirId = definitionHirId)

        astToHirMap[variable.nodeId] = hirId

        return hirVar
    }

    private fun convertBinary(binary: Binary, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val operator = when (binary.operator) {
            "+" -> BinaryOperator.ADD
            "-" -> BinaryOperator.SUB
            "*" -> BinaryOperator.MUL
            "/" -> BinaryOperator.DIV
            "%" -> BinaryOperator.MOD
            "==" -> BinaryOperator.EQ
            "!=" -> BinaryOperator.NEQ
            "<" -> BinaryOperator.LT
            "<=" -> BinaryOperator.LTE
            ">" -> BinaryOperator.GT
            ">=" -> BinaryOperator.GTE
            "&&" -> BinaryOperator.AND
            "||" -> BinaryOperator.OR
            ".." -> BinaryOperator.INCLUSIVE_RANGE
            "..<" -> BinaryOperator.EXCLUSIVE_RANGE

            else -> error("Unknown binary operator: ${binary.operator}")
        }

        val left = convertExpression(binary.left)
        val right = convertExpression(binary.right)

        val hirBinary = HIRBinaryOp(
            hirId = hirId,
            type = type,
            operator = operator,
            left = left,
            right = right)

        astToHirMap[binary.nodeId] = hirId
        return hirBinary
    }

    private fun convertUnary(unary: Unary, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val operator = when (unary.operator) {
            "-" -> UnaryOperator.NEGATE
            "!" -> UnaryOperator.NOT
            else -> error("Unknown unary operator: ${unary.operator}")
        }

        val operand = convertExpression(unary.expr)

        val hirUnary = HIRUnaryOp(
            hirId = hirId,
            type = type,
            operator = operator,
            operand = operand)

        astToHirMap[unary.nodeId] = hirId

        return hirUnary
    }

    private fun convertCall(call: Call, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val callee = convertExpression(call.callee)
        val arguments = call.args.map { arg ->
            HIRArgument(
                name = arg.name,
                value = convertExpression(arg.expr))
        }

        val hirCall = HIRCall(
            hirId = hirId,
            type = type,
            callee = callee,
            arguments = arguments)

        astToHirMap[call.nodeId] = hirId

        return hirCall
    }

    private fun convertGet(get: Get, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val receiver = convertExpression(get.receiver)
        val receiverClassName = extractClassName(typeResolution[get.receiver.nodeId])
        val fieldOffset = computeFieldOffset(receiverClassName, get.name)

        val hirGet = HIRFieldAccess(
            hirId = hirId,
            type = type,
            receiver = receiver,
            fieldName = get.name,
            fieldOffset = fieldOffset,
            receiverClassName = receiverClassName)

        astToHirMap[get.nodeId] = hirId

        return hirGet
    }

    private fun convertSet(set: Set, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val receiver = convertExpression(set.receiver)
        val receiverClassName = extractClassName(typeResolution[set.receiver.nodeId])
        val fieldOffset = computeFieldOffset(receiverClassName, set.name)
        val value = convertExpression(set.value)

        val hirSet = HIRAssign(
            hirId = hirId,
            type = type,
            target = FieldTarget(receiver, set.name, fieldOffset),
            value = value)

        astToHirMap[set.nodeId] = hirId

        return hirSet
    }

    private fun convertAssign(assign: Assign, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val value = convertExpression(assign.value)

        val hirAssign = HIRAssign(
            hirId = hirId,
            type = type,
            target = VariableTarget(assign.name),
            value = value)

        astToHirMap[assign.nodeId] = hirId

        return hirAssign
    }

    private fun convertConditional(conditional: Conditional, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()
        val condition = convertExpression(conditional.condition)
        val thenBranch = HIRBlock(allocateHirId(), listOf(convertStatement(conditional.thenBranch)))
        val elseBranch = conditional.elseBranch?.let { HIRBlock(allocateHirId(), listOf(convertStatement(it))) }

        val hirConditional = HIRConditional(
            hirId = hirId,
            type = type,
            condition = condition,
            thenBranch = thenBranch,
            elseBranch = elseBranch)

        astToHirMap[conditional.nodeId] = hirId

        return hirConditional
    }

    private fun convertLambda(lambda: Lambda, type: HIRType) : HIRExpression {
        val hirId = allocateHirId()

        val parameters = lambda.parameters.map { param ->
            HIRLambdaParameter(
                name = param.name,
                type = convertType(param.type))
        }

        val captures = lambdaClosures[lambda.nodeId] ?: emptyMap()
        val capturedVariables = captures.map { (name, definitionNodeId) ->
            val definitionHirId = astToHirMap[definitionNodeId] ?: -1
            val captureType = typeResolution[definitionNodeId]?.let { convertType(it) } ?: HIRAnyType

            HIRCapturedVariable(
                name = name,
                type = captureType,
                definitionHirId = definitionHirId)
        }

        val body = lambda.body.map { convertStatement(it) }

        val hirLambda = HIRLambda(
            hirId = hirId,
            type = type,
            parameters = parameters,
            capturedVariables = capturedVariables,
            body = body)

        astToHirMap[lambda.nodeId] = hirId

        return hirLambda
    }


    // ========================================================================
    // TYPE CONVERSION
    // ========================================================================

    private fun convertType(parserType: ParserType) : HIRType {
        return convertParserTypeToHIRType(parserType)
    }


    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================

    private fun extractClassName(parserType: ParserType?) : String {
        return when (parserType) {
            is ObjectType -> parserType.className
            else -> "Unknown"
        }
    }

    private fun computeFieldOffset(className: String, fieldName: String) : Int {
        val classId = symbolTable.lookupNodeId(className)
            ?: return -1

        val symbol = symbolTable.getSymbol(classId)

        if (symbol !is ClassSymbol)
            return -1

        var offset = 0

        for (field in symbol.signature.fields) {
            if (field.key == fieldName) return offset
            offset += 8
        }

        return -1
    }
}
