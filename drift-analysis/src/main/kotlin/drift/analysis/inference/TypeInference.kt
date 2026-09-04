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
import drift.ast.NodeId
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.types.*
import drift.values.*
import drift.values.containers.list.ArrayValue
import drift.values.primaries.BoolValue
import drift.values.primaries.IntValue
import drift.values.primaries.Int64Value
import drift.values.primaries.NumericValue
import drift.values.primaries.StringValue
import drift.values.primaries.UIntValue
import drift.values.specials.NotAssignedValue
import drift.values.primaries.NullValue
import drift.values.specials.VoidValue
import language.ModuleReference
import language.Namespace
import language.QualifiedName


class TypeInference(
    val namespace: Namespace,
    val ast: List<ParserStatement>,
    val symbolTable: SymbolTable,
    val refResolutions: Map<NodeId, NodeId>) {

    private val typeResolutions = mutableMapOf<NodeId, Type>()


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

    private fun inferLet(let: Let) : Type {
        let.run {
            val valueType = inferExpression(value)
            val expectedVariableType =
                if (type == AnyType) valueType
                else type.resolve(ModuleReference.unresolved, namespace)

            // Store the expression's own type.
            value?.let { typeResolutions[it.nodeId] = valueType }

            // Store the variable's expected type.
            typeResolutions[nodeId] = expectedVariableType

            // NOTE: the types of the variable and its own value must be
            //  compared on type checking to ensure the integrity of the
            //  structure's type constraint.
        }

        return VoidType
    }

    private fun inferIf(`if`: If) : InferenceResult {
        val resolvedReturnTypes = mutableSetOf<Type>()

        `if`.run {
            inferExpression(condition)
            resolvedReturnTypes.addAll(inferStatement(thenBranch).resolvedReturnTypes)
            elseBranch?.let {
                resolvedReturnTypes.addAll(inferStatement(it).resolvedReturnTypes)
            }
        }

        return InferenceResult(resolvedReturnTypes)
    }

    private fun inferReturn(`return`: Return) : Type {
        return inferExpression(`return`.value)
    }

    private fun inferBlock(block: Block) : InferenceResult {
        val resolvedReturnTypes = mutableSetOf<Type>()
        var lastType: Type = VoidType

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

    private fun inferFor(`for`: For) : Type {
        `for`.run {
            inferExpression(iterable)
            inferStatement(body)
        }

        return VoidType
    }

    private fun inferFunction(func: Func) : Type {
        func.run {
            parameters.forEach { parameter ->
                parameter.defaultValue?.let { inferExpression(it) }
            }

            val inferredBlock = inferBlock(body)

            typeResolutions[nodeId] = when (val declaredReturnType = func.returnType) {
                is AnyType -> buildType(inferredBlock.resolvedReturnTypes)
                is LastType -> inferredBlock.lastType
                else -> declaredReturnType.resolve(ModuleReference.unresolved, namespace)
            }
        }

        return VoidType
    }

    private fun buildType(types: Collection<Type>) : Type {
        return if (types.size == 1) {
            types.first()
        } else if (types.size > 1) {
            UnionType(types.toList())
        } else {
            VoidType
        }
    }

    private fun inferClass(`class`: Class) : Type {
        `class`.run {
            fields.forEach { inferStatement(it) }
            staticFields.forEach { inferStatement(it) }
            methods.forEach { inferStatement(it) }
            staticMethods.forEach { inferStatement(it) }
        }

        return VoidType
    }

    private fun inferExprStmt(exprStmt: ExprStmt) : Type {
        return inferExpression(exprStmt.expr)
    }


    /*  --  EXPRESSIONS  --  */

    private fun inferExpression(expression: ParserExpression?) : Type {
        return when (expression) {
            is Reference -> inferReference(expression)
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

    private fun inferReference(reference: Reference) : Type {
        val referenceNodeId = reference.nodeId
        val definitionNodeId = refResolutions[referenceNodeId]
            ?: return UnknownType // TODO: throw

        val type: Type = when (val symbol = symbolTable.getSymbol(definitionNodeId)) {
            is CallableSymbol -> {
                val returnTypes = typeResolutions[definitionNodeId]
                    ?: throw DIRNotDefinedSymbolException(name = "nodeId#$definitionNodeId")

                val functionType = FunctionType(
                    paramTypes = symbol.signature.parameterTypes.map { it.type },
                    returnType = returnTypes)

                typeResolutions[referenceNodeId] = functionType
                functionType
            }
            is ClassSymbol -> ClassType(symbol.signature.qualifiedName)
            is VariableSymbol -> typeResolutions[definitionNodeId] ?: symbol.signature.type

            else -> throw DIRUnexpectedExpressionException()
        }

        typeResolutions[referenceNodeId] = type

        return type
    }

    private fun inferLiteral(literal: Literal) : Type {
        fun obj(primitive: ParserPrimitiveClass) =
            ObjectType(primitive)

        val type: Type = when (literal.value) {
            is NumericValue        -> {
                val v = (literal.value as NumericValue).value
                if (v >= Int.MIN_VALUE && v <= Int.MAX_VALUE)
                    obj(ParserPrimitiveClass.Int)
                else
                    obj(ParserPrimitiveClass.Int64)
            }
            is IntValue            -> obj(ParserPrimitiveClass.Int)
            is Int64Value          -> obj(ParserPrimitiveClass.Int64)
            is UIntValue           -> obj(ParserPrimitiveClass.UInt)
            is StringValue         -> obj(ParserPrimitiveClass.String)
            is BoolValue           -> obj(ParserPrimitiveClass.Bool)
            is ArrayValue          -> obj(ParserPrimitiveClass.Array)
            is NullValue           -> NullType
            is NotAssignedValue    -> UnknownType
            is VoidValue           -> VoidType
            // TODO: clean up deprecated/impossible branches.
        }

        typeResolutions[literal.nodeId] = type

        return type
    }

    private fun inferUnary(unary: Unary) : Type {
        val exprType = inferExpression(unary.expr)

        if (exprType is VoidType) {
            throw DIRUnexpectedVoidTypeException()
        } else if (exprType is UnknownType) {
            throw DIRUnexpectedUnknownTypeException()
        }

        val isNumericType = exprType is ObjectType &&
                            exprType.isPrimitiveNumeric()
        val isBooleanType = exprType is ObjectType &&
                            exprType.isPrimitiveBool()

        val type: Type = when (val operator = unary.operator) {
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

    private fun inferBinary(binary: Binary) : Type {
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

        val type: Type = when (binary.operator) {
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
                    ObjectType(ParserPrimitiveClass.Bool)
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

            "==", "!=" -> ObjectType(ParserPrimitiveClass.Bool)

            ".." -> {
                if (!isNumericType) {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }

                ObjectType(
                    QualifiedName(module = ModuleReference.homemade, simpleName = "InclusiveRange"),
                    mapOf("limitType" to SingleType(promoteNumericTypes(leftType, rightType))))
            }

            "..<" -> {
                if (!isNumericType) {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType)
                    )
                }

                ObjectType(
                    QualifiedName(module = ModuleReference.homemade, simpleName = "ExclusiveRange"),
                    mapOf("limitType" to SingleType(promoteNumericTypes(leftType, rightType))))
            }

            else -> throw DIRUnsupportedOperationException(
                operator = binary.operator,
                types = Pair(leftType, rightType))
        }

        typeResolutions[binary.nodeId] = type

        return type
    }

    private fun inferConditional(conditional: Conditional) : Type {
        val conditionType = inferExpression(conditional.condition)

        if (conditionType !is ObjectType || !conditionType.isPrimitiveBool())
            throw DIRUnexpectedTypeException()

        val thenType = inferStatement(conditional.thenBranch).lastType
        val elseType =
            if (conditional.elseBranch != null) inferStatement(conditional.elseBranch!!).lastType
            else NullType       // NOTE: if none else branch and condition equals FALSE,
                                //  Null is implicitly returned

        val type: Type = when {
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

    private fun inferAssign(assign: Assign) : Type {
        val type = inferExpression(assign.value)

        typeResolutions[assign.nodeId] = type

        return type     // NOTE: return the type on assign is necessary to support
                        //       the assign chaining 'x = y = value'
    }

    private fun inferCall(call: Call) : Type {
        val callee = call.callee


        fun handleVariable(callee: Reference): Type {
            val defId = refResolutions[callee.nodeId]
                ?: return UnknownType       // NOTE: if there isn't any ref, the structure
                                            //  isn't initialized (none ref linked to declaration)

            val type: Type = when (val symbol = symbolTable.getSymbol(defId)) {
                is CallableSymbol -> {
                    symbol.signature.parameterTypes.zip(call.args).forEach { (param, arg) ->
                        val argExpr = arg.expr
                        if (argExpr is Literal && argExpr.value is NumericValue) {
                            typeResolutions[argExpr.nodeId] = param.type
                        }
                    }
                    
                    typeResolutions[defId]
                        ?: throw DIRNotDefinedSymbolException(name = "nodeId#$defId")
                }
                is ClassSymbol -> ObjectType(symbol.signature.qualifiedName)
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

        fun handleAccessor(callee: Get): Type {
            val type = typeResolutions[callee.nodeId]
                ?: throw DIRUnexpectedUnknownTypeException()

            if (type !is FunctionType)
                throw DIRUnexpectedTypeException()

            type.paramTypes
                .zip(call.args)
                .forEach { (paramType, arg) ->
                    val argExpr = arg.expr

                    if (argExpr is Literal && argExpr.value is NumericValue) {
                        typeResolutions[argExpr.nodeId] = paramType
                    }
                }

            typeResolutions[call.nodeId] = type.returnType

            return type.returnType
        }


        call.args.forEach { inferExpression(it.expr) }

        inferExpression(callee)

        return when (callee) {
            is Reference    -> handleVariable(callee)
            is Get          -> handleAccessor(callee)

            else            -> throw DTCUnexpectedCalleeException()
        }
    }

    private fun inferGet(get: Get) : Type {
        val receiverType = inferExpression(get.receiver)

        val type: Type = when (receiverType) {
            is ObjectType -> {
                val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                    ?: throw DIRNotDefinedClassException(name = receiverType.qualifiedName.qualifiedName)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                classRef.signature.fields[get.name]
                    ?: classRef.signature.methods[get.name]?.let { ctx ->
                        FunctionType(
                            paramTypes = ctx.parameterTypes.map { it.type },
                            returnType = ctx.returnType)
                    }
                    ?: throw DIRNotDefinedSymbolException(
                        name = "(instance of ${classRef.signature.qualifiedName}).${get.name}")
            }
            is ClassType -> {
                val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                    ?: throw DIRNotDefinedClassException(name = receiverType.qualifiedName.qualifiedName)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                classRef.signature.staticFields[get.name]
                    ?: classRef.signature.staticMethods[get.name]?.let { ctx ->
                        FunctionType(
                            paramTypes = ctx.parameterTypes.map { it.type },
                            returnType = ctx.returnType)
                    }
                    ?: throw DIRNotDefinedSymbolException(
                        name = "${classRef.signature.qualifiedName}.${get.name}")
            }

            else -> throw DIRUnexpectedTypeException()
        }

        typeResolutions[get.nodeId] = type

        return type
    }

    private fun inferSet(set: Set) : Type {
        val receiverType = inferExpression(set.receiver)

        val type: Type = when (receiverType) {
            is ObjectType -> {
                val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                    ?: throw DIRNotDefinedClassException(name = receiverType.qualifiedName.qualifiedName)

                val classRef = symbolTable.getSymbol(classId) as ClassSymbol

                val type = classRef.signature.fields[set.name]
                    ?: throw DIRNotDefinedSymbolException(name = set.name)

                val valueType = inferExpression(set.value)

                if (!isAssignable(valueType, type))
                    throw DIRUnexpectedTypeException()

                type
            }
            is ClassType -> {
                val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                    ?: throw DIRNotDefinedClassException(name = receiverType.qualifiedName.qualifiedName)

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

    private fun inferLambda(lambda: Lambda) : Type {
        val type: Type

        lambda.run {
            parameters.forEach { parameter ->
                parameter.defaultValue?.let { defValue -> inferExpression(defValue) }
            }

            val paramTypes = parameters.map { it.type.resolve(ModuleReference.unresolved, namespace) }
            val inferredBlock = inferBlock(body)

            val resolvedReturnType: Type = when (val declaredReturnType = returnType) {
                is AnyType -> {
                    val resolvedReturnTypes = inferredBlock
                        .resolvedReturnTypes

                    buildType(resolvedReturnTypes)
                }
                is LastType -> inferredBlock.lastType

                else -> declaredReturnType.resolve(ModuleReference.unresolved, namespace)
            }

            type = FunctionType(paramTypes, resolvedReturnType)

            typeResolutions[nodeId] = type
        }

        return type
    }

    private fun inferArray(list: drift.ast.expressions.Array) : Type {
        var firstType: Type = AnyType

        if (list.values.isNotEmpty()) {
            firstType = inferExpression(list.values.first())

            list.values.subList(1, list.values.size)
                .forEach {
                    if (inferExpression(it) != firstType)
                        throw DIRUnexpectedTypeException()
                }
        }

        val type = ObjectType(
            ParserPrimitiveClass.Array,
            args = mapOf("elementType" to SingleType(firstType)))

        typeResolutions[list.nodeId] = type

        return type
    }


    private data class InferenceResult(
        val resolvedReturnTypes: kotlin.collections.Set<Type> = emptySet(),
        val lastType: Type = VoidType)

    data class TypeInferenceResult(
        val typeResolutions: Map<NodeId, Type>) {

        companion object {

            fun empty() = TypeInferenceResult(
                typeResolutions = emptyMap())
        }
    }
}