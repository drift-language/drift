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
import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.metadata.Annotation
import drift.ast.statements.*
import drift.ast.statements.hooks.Hook
import drift.ast.statements.hooks.ReturnableHook
import drift.ast.statements.hooks.UnreturnableHook
import drift.hir.exceptions.DHIRUnsupported
import drift.hir.metadata.HIRAnnotation
import drift.oldruntime.ParserType
import drift.oldruntime.AnyType
import drift.oldruntime.ClassType
import drift.oldruntime.ObjectType
import language.LangInfo.NAMESPACE_SEPARATOR

/**
 * Converter from Drift AST to HIR (High-level Intermediate Representation).
 */
class HIRConverter(
    private val namespace: String,
    private val ast: List<ParserStatement>,
    private val symbolTable: SymbolTable,
    private val refResolutions: Map<Int, Int>,
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

    private val classMethodHirIds = mutableMapOf<String, Int>()


    fun convert() : List<HIRStatement> {
        return ast.map { convertStatement(it) }
    }


    // ========================================================================
    // STATEMENTS
    // ========================================================================

    private fun convertStatement(stmt: ParserStatement) : HIRStatement {
        return when (stmt) {
            is Let -> convertLet(stmt)
            is Func -> convertFunction(stmt, isStatic = true)
                    // NOTE: isStatic is set to TRUE in this context
                    //  because it defines a top-level function, not a
                    //  class method.
                    //  A top-level function, in JVM, is declared in
                    //  a synthetic class as a static method.
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

    private fun convertLet(let: Let) : HIRVariable {
        val hirId = allocateHirId()

        val annotations = let.annotations
            .map(this::convertAnnotation)
            .toMutableList()

        val type = convertType(typeResolution[let.nodeId] ?: let.type)

        val initialValue = let.value?.let(this::convertExpression)

        val hirVar = HIRVariable(
            hirId = hirId,
            annotations = annotations,
            name = let.name,
            type = type,
            initialValue = initialValue,
            isMutable = let.isMutable)

        astToHirMap[let.nodeId] = hirId

        return hirVar
    }

    private fun convertFunction(
        function: Func,
        isStatic: Boolean,
        receiverQualifiedName: String? = null)
    : HIRFunction {

        val hirId = allocateHirId()

        val annotations = function.annotations
            .map(this::convertAnnotation)
            .toMutableList()

        val returnType = convertType(function.returnType)

        val parameters = function
            .parameters
            .map(this::convertParameter)

        val body = function
            .body
            .statements
            .map(this::convertStatement)

        val name =
            if (receiverQualifiedName != null) "$receiverQualifiedName$NAMESPACE_SEPARATOR${function.name}"
            else function.name

        val hirFunc = HIRFunction(
            hirId = hirId,
            annotations = annotations,
            name = name,
            parameters = parameters,
            returnType = returnType,
            body = body,
            isStatic = isStatic)

        astToHirMap[function.nodeId] = hirId

        if (receiverQualifiedName != null)
            classMethodHirIds[name] = hirId

        return hirFunc
    }

    private fun convertHook(hook: Hook) : HIRHook {
        val hirId = allocateHirId()

        val returnType =
            if (hook is ReturnableHook) convertType(hook.returnType)
            else HIRPrimitiveType(PrimitiveKind.VOID)

        val parameters = hook
            .parameters
            .map(this::convertParameter)

        val body = hook
            .body
            .statements
            .map(this::convertStatement)

        val hirHook = HIRHook(
            hirId,
            hook.name,
            parameters,
            returnType,
            body)

        if (hook !is ReturnableHook && hook !is UnreturnableHook)
            error("Unexpected hook type")

        astToHirMap[hook.nodeId] = hirId

        return hirHook
    }

    private fun convertParameter(param: FunctionParameter) : HIRParameter {
        val paramHirId = allocateHirId()

        astToHirMap[param.nodeId] = paramHirId

        return HIRParameter(
            hirId = paramHirId,
            name = param.name,
            type = convertType(param.type),
            defaultValue = param.defaultValue?.let { convertExpression(it) })
    }

    private fun convertClass(clazz: Class) : HIRClass {
        val hirId = allocateHirId()

        val annotations = clazz.annotations
            .map(this::convertAnnotation)
            .toMutableList()

        val classQualifiedName = "$namespace$NAMESPACE_SEPARATOR${clazz.name}"

        val staticFields = clazz.staticFields.map { convertClassField(it, isStatic = true) }
        val fields = clazz.fields.map { convertClassField(it, isStatic = false) }
        val staticMethods = clazz.staticMethods.map {
            convertFunction(
                it,
                isStatic = true,
                receiverQualifiedName = classQualifiedName)
        }
        val methods = clazz.methods.map {
            convertFunction(
                it,
                isStatic = false,
                receiverQualifiedName = classQualifiedName)
        }
        val hooks = clazz.hooks.map { convertHook(it) }

        val hirClass = HIRClass(
            hirId = hirId,
            annotations = annotations,
            name = clazz.name,
            fields = fields,
            methods = methods,
            hooks = hooks,
            staticFields = staticFields,
            staticMethods = staticMethods)

        astToHirMap[clazz.nodeId] = hirId

        return hirClass
    }

    private fun convertClassField(field: Let, isStatic: Boolean) : HIRField {
        val hirId = allocateHirId()
        val fieldAnnotations = field.annotations
            .map(this::convertAnnotation)
            .toMutableList()

        return HIRField(
            hirId = hirId,
            name = field.name,
            annotations = fieldAnnotations,
            type = convertType(field.type),
            isStatic = isStatic)
    }

    private fun convertIfStmt(ifStmt: If) : HIRExpressionStmt {
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

    private fun convertFor(forLoop: For) : HIRExpressionStmt {
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

    private fun convertReturn(returnStmt: Return) : HIRReturn {
        val hirId = allocateHirId()
        val value = returnStmt.value?.let(this::convertExpression)

        val hirReturn = HIRReturn(
            hirId = hirId,
            value = value)

        astToHirMap[returnStmt.nodeId] = hirId
        return hirReturn
    }

    private fun convertBlock(block: Block) : HIRBlock {
        val hirId = allocateHirId()
        val statements = block.statements.map { convertStatement(it) }

        val hirBlock = HIRBlock(
            hirId = hirId,
            statements = statements)

        astToHirMap[block.nodeId] = hirId
        return hirBlock
    }

    private fun convertExprStmt(exprStmt: ExprStmt) : HIRExpressionStmt {
        val hirId = allocateHirId()
        val expression = convertExpression(exprStmt.expr)

        val hirExprStmt = HIRExpressionStmt(
            hirId = hirId,
            expression = expression)

        astToHirMap[exprStmt.nodeId] = hirId
        return hirExprStmt
    }

    private fun convertImport(importStmt: Import) : HIRImport {
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
            is Reference -> convertVariable(expr, type)
            is Binary -> convertBinary(expr, type)
            is Unary -> convertUnary(expr, type)
            is Call -> convertCall(expr, type)
            is Get -> convertGet(expr)
            is Set -> convertSet(expr, type)
            is Assign -> convertAssign(expr, type)
            is Conditional -> convertConditional(expr, type)
            is Lambda -> convertLambda(expr, type)
            is drift.ast.expressions.Array -> throw DHIRUnsupported("Array is not supported yet")

            else -> error("Unknown expression type: ${expr::class.simpleName}")
        }
    }

    private fun convertLiteral(literal: Literal, type: HIRType) : HIRLiteral {
        val hirId = allocateHirId()
        val value = literal.value.value

        val hirLiteral = HIRLiteral(
            hirId = hirId,
            type = type,
            value = value)

        astToHirMap[literal.nodeId] = hirId

        return hirLiteral
    }

    private fun convertVariable(reference: Reference, type: HIRType) : HIRVariableRef {
        val hirId = allocateHirId()

        val definitionHirId = getDefinitionHirIdFromRefResolutions(reference.nodeId)

        val hirVar = HIRVariableRef(
            hirId = hirId,
            type = type,
            name = reference.name,
            definitionHirId = definitionHirId)

        astToHirMap[reference.nodeId] = hirId

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
            "..",
            "..<" -> return convertRange(binary)

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

    private fun convertRange(rangeOperation: Binary) : HIRCall {
        TODO("Ranges are not implemented yet")
    }

    private fun convertUnary(unary: Unary, type: HIRType) : HIRUnaryOp {
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

    private fun convertCall(call: Call, type: HIRType) : HIRCall {
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

    private fun convertGet(get: Get) : HIRAccess {
        val hirId = allocateHirId()

        val receiver = convertExpression(get.receiver)
        val receiverType = typeResolution[get.receiver.nodeId]
        val receiverClassName = extractClassName(receiverType)
        val receiverClassNodeId = symbolTable.lookupNodeId(receiverClassName)
            ?: error("Class '$receiverClassName' not found")
        val receiverStructure = symbolTable.getSymbol(receiverClassNodeId)
        val receiverClass = receiverStructure as? ClassSymbol
            ?: error("Unexpected receiver type")

        val fieldOffset = computeFieldOffset(receiverClassName, get.name)


        fun handleStaticMember() : HIRStaticAccess {
            with(receiverClass.signature) {
                staticFields[get.name]?.let {
                    return HIRStaticFieldAccess(
                        hirId = hirId,
                        type = convertType(it),
                        receiverClassName = receiverClassName,
                        memberName = get.name)
                }

                staticMethods[get.name]?.let {
                    val methodQualifiedName =
                        "$receiverClassName$NAMESPACE_SEPARATOR${get.name}"
                    val definitionHirId = classMethodHirIds[methodQualifiedName]
                        ?: error("Undefined static method")

                    return HIRStaticMethodAccess(
                        hirId = hirId,
                        type = convertType(it.returnType),
                        receiverClassName = receiverClassName,
                        memberName = get.name,
                        definitionHirId = definitionHirId)
                }

                error("Member not found in static context")
            }
        }
        fun handleInstanceMember() : HIRInstanceAccess {
            with(receiverClass.signature) {
                fields[get.name]?.let {
                    return HIRFieldAccess(
                        hirId = hirId,
                        type = convertType(it),
                        receiver = receiver,
                        receiverClassName = receiverClassName,
                        memberName = get.name,
                        memberOffset = fieldOffset)
                }

                methods[get.name]?.let {
                    val methodQualifiedName =
                        "$receiverClassName$NAMESPACE_SEPARATOR${get.name}"
                    val definitionHirId = classMethodHirIds[methodQualifiedName]
                        ?: error("Undefined instance method")

                    return HIRMethodAccess(
                        hirId = hirId,
                        type = convertType(it.returnType),
                        receiver = receiver,
                        receiverClassName = receiverClassName,
                        memberName = get.name,
                        memberOffset = fieldOffset,
                        definitionHirId = definitionHirId)
                }

                error("Member not found in instance context")
            }
        }


        val accessNode: HIRAccess = when (receiverType) {
            is ClassType    -> handleStaticMember()
            is ObjectType   -> handleInstanceMember()

            else            -> error("Unexpected receiver type")
        }

        astToHirMap[get.nodeId] = hirId

        return accessNode
    }

    private fun convertSet(set: Set, type: HIRType) : HIRAssign {
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

    private fun convertAssign(assign: Assign, type: HIRType) : HIRAssign {
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

    private fun convertConditional(conditional: Conditional, type: HIRType) : HIRConditional {
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

    private fun convertLambda(lambda: Lambda, type: HIRType) : HIRLambda {
        val hirId = allocateHirId()

        val parameters = lambda.parameters.map { param ->
            HIRLambdaParameter(
                name = param.name,
                type = convertType(param.type))
        }

        val captures = lambdaClosures[lambda.nodeId] ?: emptyMap()
        val capturedVariables = captures.map { (name, definitionNodeId) ->
            val definitionHirId = astToHirMap[definitionNodeId]
            val captureType = typeResolution[definitionNodeId]?.let { convertType(it) } ?: HIRAnyType

            HIRCapturedVariable(
                name = name,
                type = captureType,
                definitionHirId = definitionHirId)
        }

        val body = lambda
            .body
            .statements
            .map { convertStatement(it) }

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
    // EXPRESSIONS
    // ========================================================================

    private fun convertAnnotation(annotation: Annotation) : HIRAnnotation {
        return HIRAnnotation(
            name = annotation.name,
            args = annotation.args.map { argument ->
                HIRArgument(
                    name = argument.name,
                    value = convertExpression(argument.expr))
            })
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
            is ClassType  -> parserType.className

            else -> $$"$Unknown$"
        }
    }

    private fun computeFieldOffset(className: String, memberName: String) : Int {
        val classId = symbolTable.lookupNodeId(className)
            ?: return -1

        val symbol = symbolTable.getSymbol(classId) as? ClassSymbol
            ?: return -1

        var offset = 0

        for (field in symbol.signature.fields) {
            if (field.key == memberName)
                return offset

            offset += 8
        }

        for (method in symbol.signature.methods) {
            if (method.key == memberName)
                return offset

            offset += 8
        }

        return -1
    }

    private fun getDefinitionHirIdFromRefResolutions(refNodeId: Int) : Int {
        val definitionNodeId = refResolutions[refNodeId]
            ?: error("Undefined reference")

        return astToHirMap[definitionNodeId]
            ?: error("Reference definition not found")
    }
}
