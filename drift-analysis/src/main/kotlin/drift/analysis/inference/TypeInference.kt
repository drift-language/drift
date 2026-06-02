/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.inference

import drift.analysis.exceptions.*
import drift.analysis.symbols.CallableSymbol
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.analysis.symbols.VariableSymbol
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.oldruntime.*
import drift.oldruntime.values.containers.list.ParserArray
import drift.oldruntime.values.primaries.ParserBool
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.primaries.ParserInt64
import drift.oldruntime.values.primaries.ParserNumeric
import drift.oldruntime.values.primaries.ParserString
import drift.oldruntime.values.primaries.ParserUInt
import drift.oldruntime.values.specials.ParserNotAssigned
import drift.oldruntime.values.primaries.ParserNull
import drift.oldruntime.values.specials.ParserVoid

class TypeInference(
    val ast: List<ParserStatement>,
    val symbolTable: SymbolTable,
    val refResolutions: Map<Int, Int>) {

    private val typeResolutions = mutableMapOf<Int, ParserType>()


    fun infer() : TypeInferenceResult {
        ast.forEach { inferStatement(it) }

        return TypeInferenceResult(typeResolutions)
    }


    /*  --  STATEMENTS  --  */

    private fun inferStatement(statement: ParserStatement) : InferenceResult {
        return when (statement) {
            is Let      -> {
                inferLet(statement)

                InferenceResult()
            }
            is If       -> inferIf(statement)
            is Return   -> {
                val type = inferReturn(statement)

                InferenceResult(
                    resolvedReturnTypes = setOf(type),
                    lastType = type)
            }
            is Block    -> inferBlock(statement)
            is For      -> {
                inferFor(statement)

                InferenceResult()
            }
            is Func     -> {
                inferFunction(statement)

                InferenceResult()
            }
            is Class    -> {
                inferClass(statement)

                InferenceResult()
            }
            is ExprStmt -> {
                val type = inferExprStmt(statement)

                InferenceResult(
                    lastType = type)
            }

            else -> InferenceResult()
        }
    }

    private fun inferLet(let: Let) : ParserType {
        let.run {
            val valueType = inferExpression(value)

            val usedType =
                if (type == AnyType) valueType
                else type

            val expr = value
            if (type != AnyType && expr is Literal && expr.value is ParserNumeric) {
                typeResolutions[expr.nodeId] = type
            }

            typeResolutions[nodeId] = usedType
        }

        return VoidType
    }

    private fun inferIf(`if`: If) : InferenceResult {
        val resolvedReturnTypes = mutableSetOf<ParserType>()

        `if`.run {
            inferExpression(condition)
            resolvedReturnTypes.addAll(inferStatement(thenBranch).resolvedReturnTypes)
            elseBranch?.let {
                resolvedReturnTypes.addAll(inferStatement(it).resolvedReturnTypes)
            }
        }

        return InferenceResult(resolvedReturnTypes)
    }

    private fun inferReturn(`return`: Return) : ParserType {
        return inferExpression(`return`.value)
    }

    private fun inferBlock(block: Block) : InferenceResult {
        val resolvedReturnTypes = mutableSetOf<ParserType>()
        var lastType: ParserType = VoidType

        block.statements.forEach {
            val inference = inferStatement(it)

            lastType = inference.lastType
            resolvedReturnTypes.addAll(inference.resolvedReturnTypes)
        }

        return InferenceResult(
            resolvedReturnTypes = resolvedReturnTypes,
            lastType = lastType
        )
    }

    private fun inferFor(`for`: For) : ParserType {
        `for`.run {
            inferExpression(iterable)
            inferStatement(body)
        }

        return VoidType
    }

    private fun inferFunction(func: Func) : ParserType {
        func.run {
            parameters.forEach { parameter ->
                parameter.defaultValue?.let { inferExpression(it) }
            }

            val inferredBlock = inferBlock(body)

            typeResolutions[nodeId] =
                if (func.returnType is AnyType) {
                    val resolvedReturnTypes = inferredBlock
                        .resolvedReturnTypes

                    buildType(resolvedReturnTypes)
                } else {
                    func.returnType
                }
        }

        return VoidType
    }

    private fun buildType(types: Collection<ParserType>) : ParserType {
        return if (types.size == 1) {
            types.first()
        } else if (types.size > 1) {
            UnionType(types.toList())
        } else {
            VoidType
        }
    }

    private fun inferClass(`class`: Class) : ParserType {
        `class`.run {
            fields.forEach { inferStatement(it) }
            staticFields.forEach { inferStatement(it) }
            methods.forEach { inferStatement(it) }
            staticMethods.forEach { inferStatement(it) }
        }

        return VoidType
    }

    private fun inferExprStmt(exprStmt: ExprStmt) : ParserType {
        return inferExpression(exprStmt.expr)
    }


    /*  --  EXPRESSIONS  --  */

    private fun inferExpression(expression: ParserExpression?) : ParserType {
        return when (expression) {
            is Variable -> inferVariable(expression)
            is Literal -> inferLiteral(expression)
            is Unary -> inferUnary(expression)
            is Binary -> inferBinary(expression)
            is Conditional -> inferConditional(expression)
            is Assign -> inferAssign(expression)
            is Call -> inferCall(expression)
            is Get -> inferGet(expression)
            is Set -> inferSet(expression)
            is Lambda -> inferLambda(expression)
            is drift.ast.expressions.Array -> inferArray(expression)

            else -> UnknownType // TODO: throw?
        }
    }

    private fun inferVariable(variable: Variable) : ParserType {
        val referenceNodeId = variable.nodeId
        val definitionNodeId = refResolutions[referenceNodeId]
            ?: return UnknownType // TODO: throw

        val type: ParserType = when (val symbol = symbolTable.getSymbol(definitionNodeId)) {
            is CallableSymbol -> {
                val returnTypes = typeResolutions[definitionNodeId]
                    ?: throw DIRNotDefinedSymbolException(name = "nodeId#$definitionNodeId")

                val functionType = FunctionType(
                    paramTypes = symbol.signature.parameterTypes.map { it.type },
                    returnType = returnTypes)

                typeResolutions[referenceNodeId] = functionType
                functionType
            }
            is ClassSymbol -> ClassType(symbol.signature.name)
            is VariableSymbol -> {
                typeResolutions[definitionNodeId]
                    ?: symbol.signature.type
            }

            else -> throw DIRUnexpectedExpressionException()
        }

        typeResolutions[referenceNodeId] = type

        return type
    }

    private fun inferLiteral(literal: Literal) : ParserType {
        fun obj(primitive: ParserPrimitiveClass) =
            ObjectType(primitive)

        val type: ParserType = when (literal.value) {
            is ParserNumeric        -> {
                val v = (literal.value as ParserNumeric).value
                if (v >= Int.MIN_VALUE && v <= Int.MAX_VALUE)
                    obj(ParserPrimitiveClass.Int)
                else
                    obj(ParserPrimitiveClass.Int64)
            }
            is ParserInt            -> obj(ParserPrimitiveClass.Int)
            is ParserInt64          -> obj(ParserPrimitiveClass.Int64)
            is ParserUInt           -> obj(ParserPrimitiveClass.UInt)
            is ParserString         -> obj(ParserPrimitiveClass.String)
            is ParserBool           -> obj(ParserPrimitiveClass.Bool)
            is ParserArray          -> obj(ParserPrimitiveClass.Array)
            is ParserNull           -> NullType
            is ParserNotAssigned    -> UnknownType
            is ParserVoid           -> VoidType

            else -> throw DIRUnexpectedTypeException()
        }

        typeResolutions[literal.nodeId] = type

        return type
    }

    private fun inferUnary(unary: Unary) : ParserType {
        val exprType = inferExpression(unary.expr)

        if (exprType is VoidType) {
            throw DIRUnexpectedVoidTypeException()
        } else if (exprType is UnknownType) {
            throw DIRUnexpectedUnknownTypeException()
        }

        val isNumericType = exprType is ObjectType &&
                            exprType.className in numericClassNames
        val isBooleanType = exprType is ObjectType &&
                            exprType.className == "Bool"

        val type: ParserType = when (val operator = unary.operator) {
            "-" -> {
                if (isNumericType) exprType
                else throw DIRUnsupportedOperationException(
                    operator = operator,
                    types = Pair(exprType, null))
            }

            "!" -> {
                if (isBooleanType) exprType
                else throw DIRUnsupportedOperationException(
                    operator = operator,
                    types = Pair(exprType, null))
            }

            else -> throw DIRUnsupportedOperationException(
                operator = operator,
                types = Pair(exprType, null))
        }

        typeResolutions[unary.nodeId] = type

        return type
    }

    private fun inferBinary(binary: Binary) : ParserType {
        val leftType = inferExpression(binary.left)
        val rightType = inferExpression(binary.right)

        if (leftType is VoidType || rightType is VoidType) {
            throw DIRUnexpectedVoidTypeException()
        } else if (leftType is UnknownType || rightType is UnknownType) {
            throw DIRUnexpectedUnknownTypeException()
        }

        val isLeftString = leftType is ObjectType &&
                           leftType.isPrimitiveString()

        val isNumericType = leftType is ObjectType &&
                            rightType is ObjectType &&
                            leftType.isPrimitiveNumeric() &&
                            rightType.isPrimitiveNumeric()

        val isBooleanType = leftType is ObjectType &&
                            rightType is ObjectType &&
                            leftType.isPrimitiveBool() &&
                            rightType.isPrimitiveBool()

        val type: ParserType = when (binary.operator) {
            "+" -> {
                if (isLeftString) {
                    leftType
                } else if (isNumericType) {
                    promoteNumericTypes(leftType, rightType)
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }
            }

            "-", "*", "/", "%" -> {
                if (isNumericType) {
                    promoteNumericTypes(leftType, rightType)
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }
            }

            "<", "<=", ">", ">=" -> {
                if (isNumericType) {
                    ObjectType("Bool")
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }
            }

            "&&", "||" -> {
                if (isBooleanType) {
                    leftType
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }
            }

            "==", "!=" -> ObjectType("Bool")

            ".." -> {
                if (!isNumericType) {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }

                ObjectType("InclusiveRange", mapOf(
                    "limitType" to SingleType(promoteNumericTypes(leftType, rightType))))
            }

            "..<" -> {
                if (!isNumericType) {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }

                ObjectType("ExclusiveRange", mapOf(
                    "limitType" to SingleType(promoteNumericTypes(leftType, rightType))))
            }

            else -> throw DIRUnsupportedOperationException(
                operator = binary.operator,
                types = Pair(leftType, rightType))
        }

        typeResolutions[binary.nodeId] = type

        return type
    }

    private fun inferConditional(conditional: Conditional) : ParserType {
        val conditionType = inferExpression(conditional.condition)

        if (conditionType !is ObjectType || conditionType.className != "Bool")
            throw DIRUnexpectedTypeException()

        val thenType = inferStatement(conditional.thenBranch).lastType
        val elseType =
            if (conditional.elseBranch != null) inferStatement(conditional.elseBranch!!).lastType
            else NullType       // NOTE: if none else branch and condition equals FALSE,
                                //  Null is implicitly returned

        val type: ParserType = when {
            thenType == elseType -> thenType

            thenType != NullType && elseType != NullType ->
                UnionType(listOf(thenType, elseType))

            thenType == NullType && elseType != NullType ->
                OptionalType(elseType)

            thenType != NullType && elseType == NullType ->
                OptionalType(thenType)

            else -> NullType
        }

        typeResolutions[conditional.nodeId] = type

        return type
    }

    private fun inferAssign(assign: Assign) : ParserType {
        val type = inferExpression(assign.value)

        typeResolutions[assign.nodeId] = type

        return type     // NOTE: return the type on assign is necessary to support
                        //       the assign chaining 'x = y = value'
    }

    private fun inferCall(call: Call) : ParserType {
        val callee = call.callee


        fun handleVariable(callee: Variable): ParserType {
            val defId = refResolutions[callee.nodeId]
                ?: return UnknownType       // NOTE: if there isn't any ref, the structure
                                            //  isn't initialized (none ref linked to declaration)

            val type: ParserType = when (val symbol = symbolTable.getSymbol(defId)) {
                is CallableSymbol -> {
                    symbol.signature.parameterTypes.zip(call.args).forEach { (param, arg) ->
                        val argExpr = arg.expr
                        if (argExpr is Literal && argExpr.value is ParserNumeric) {
                            typeResolutions[argExpr.nodeId] = param.type
                        }
                    }
                    
                    typeResolutions[defId]
                        ?: throw DIRNotDefinedSymbolException(name = "nodeId#$defId")
                }
                is ClassSymbol -> ObjectType(symbol.signature.name)
                is VariableSymbol -> {
                    val varType = typeResolutions[defId]
                        ?: throw DIRNotDefinedSymbolException(name = "nodeId#$defId")

                    if (varType is FunctionType) {
                        varType.returnType
                    } else {
                        throw DIRUnexpectedExpressionException()
                    }
                }

                else -> throw DIRUnexpectedExpressionException()
            }

            typeResolutions[call.nodeId] = type

            return type
        }

        fun handleAccessor(callee: Get): ParserType {
            val type = typeResolutions[callee.nodeId]
                ?: throw DIRUnexpectedUnknownTypeException()

            if (type !is FunctionType)
                throw DIRUnexpectedTypeException()

            type.paramTypes.zip(call.args).forEach { (paramType, arg) ->
                val argExpr = arg.expr
                if (argExpr is Literal && argExpr.value is ParserNumeric) {
                    typeResolutions[argExpr.nodeId] = paramType
                }
            }

            typeResolutions[call.nodeId] = type.returnType

            return type.returnType
        }


        call.args.forEach { inferExpression(it.expr) }

        inferExpression(callee)

        return when (callee) {
            is Variable -> handleVariable(callee)
            is Get      -> handleAccessor(callee)

            else        -> throw DTCUnexpectedCalleeException()
        }
    }

    private fun inferGet(get: Get) : ParserType {
        val receiverType = inferExpression(get.receiver)

        val type: ParserType = when (receiverType) {
            is ObjectType -> {
                val classId = symbolTable.lookupNodeId(receiverType.className)
                    ?: throw DIRNotDefinedClassException(name = receiverType.className)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                classRef.signature.fields[get.name]
                    ?: classRef.signature.methods[get.name]?.let { ctx ->
                        FunctionType(
                            paramTypes = ctx.parameterTypes.map { it.type },
                            returnType = ctx.returnType)
                    }
                    ?: throw DIRNotDefinedSymbolException(
                        name = "(instance of ${classRef.signature.name}).${get.name}")
            }
            is ClassType -> {
                val classId = symbolTable.lookupNodeId(receiverType.className)
                    ?: throw DIRNotDefinedClassException(name = receiverType.className)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                classRef.signature.staticFields[get.name]
                    ?: classRef.signature.staticMethods[get.name]?.let { ctx ->
                        FunctionType(
                            paramTypes = ctx.parameterTypes.map { it.type },
                            returnType = ctx.returnType)
                    }
                    ?: throw DIRNotDefinedSymbolException(
                        name = "${classRef.signature.name}.${get.name}")
            }

            else -> throw DIRUnexpectedTypeException()
        }

        typeResolutions[get.nodeId] = type

        return type
    }

    private fun inferSet(set: Set) : ParserType {
        val receiverType = inferExpression(set.receiver)

        val type: ParserType = when (receiverType) {
            is ObjectType -> {
                val classId = symbolTable.lookupNodeId(receiverType.className)
                    ?: throw DIRNotDefinedClassException(name = receiverType.className)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                val type = classRef.signature.fields[set.name]
                    ?: throw DIRNotDefinedSymbolException(name = set.name)

                val valueType = inferExpression(set.value)

                if (!isAssignable(valueType, type))
                    throw DIRUnexpectedTypeException()

                type
            }
            is ClassType -> {
                val classId = symbolTable.lookupNodeId(receiverType.className)
                    ?: throw DIRNotDefinedClassException(name = receiverType.className)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                val type = classRef.signature.staticFields[set.name]
                    ?: throw DIRNotDefinedSymbolException(name = set.name)

                val valueType = inferExpression(set.value)

                if (!isAssignable(valueType, type))
                    throw DIRUnexpectedTypeException()

                type
            }

            else -> throw DIRUnexpectedTypeException()
        }

        typeResolutions[set.nodeId] = type

        return type
    }

    private fun inferLambda(lambda: Lambda) : ParserType {
        val type: ParserType

        lambda.run {
            parameters.forEach { parameter ->
                parameter.defaultValue?.let { defValue -> inferExpression(defValue) }
            }

            val paramTypes = parameters.map { it.type }
            val inferredBlock = inferBlock(body)

            val returnType: ParserType = when (returnType) {
                is AnyType -> {
                    val resolvedReturnTypes = inferredBlock
                        .resolvedReturnTypes

                    buildType(resolvedReturnTypes)
                }
                is LastType -> inferredBlock.lastType

                else -> returnType
            }

            type = FunctionType(paramTypes, returnType)

            typeResolutions[nodeId] = type
        }

        return type
    }

    private fun inferArray(list: drift.ast.expressions.Array) : ParserType {
        var firstType: ParserType = AnyType

        if (list.values.isNotEmpty()) {
            firstType = inferExpression(list.values.first())

            list.values.subList(1, list.values.size)
                .forEach {
                    if (inferExpression(it) != firstType)
                        throw DIRUnexpectedTypeException()
                }
        }

        val type = ArrayType(type = firstType)

        typeResolutions[list.nodeId] = type

        return type
    }


    private data class InferenceResult(
        val resolvedReturnTypes: kotlin.collections.Set<ParserType> = emptySet(),
        val lastType: ParserType = VoidType)

    data class TypeInferenceResult(
        val typeResolutions: Map<Int, ParserType>) {

        companion object {

            fun empty() = TypeInferenceResult(
                typeResolutions = emptyMap())
        }
    }
}