/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.checkers

import drift.analysis.exceptions.DTCCannotReturnValueInNonReturnableContextException
import drift.analysis.exceptions.DTCClassNotFoundException
import drift.analysis.exceptions.DTCInvalidArgsCountException
import drift.analysis.exceptions.DTCMissingNamedParameterException
import drift.analysis.exceptions.DTCMissingPositionalParameterException
import drift.analysis.exceptions.DTCRefResolutionNotFoundException
import drift.analysis.exceptions.DTCTypeResolutionNotFoundException
import drift.analysis.exceptions.DTCUnexpectedCalleeException
import drift.analysis.exceptions.DTCUnexpectedReturnStatementException
import drift.analysis.exceptions.DTCUnexpectedTypeException
import drift.analysis.exceptions.DTCUnsupportedIterationException
import drift.analysis.inference.TypeInference
import drift.analysis.symbols.CallableSymbol
import drift.analysis.symbols.CallableSymbol.CallableSignature
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.analysis.symbols.VariableSymbol
import drift.ast.NodeId
import drift.ast.ParserCallable
import drift.ast.ParserReturnable
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.metadata.Annotation
import drift.ast.statements.*
import drift.types.AnyType
import drift.types.ClassType
import drift.types.FunctionType
import drift.types.LastType
import drift.types.NullType
import drift.types.ObjectType
import drift.types.OptionalType
import drift.types.Type
import drift.types.UnionType
import drift.types.UnknownType
import drift.types.UnresolvedObjectType
import drift.types.UnresolvedOptional
import drift.types.UnresolvedType
import drift.types.UnresolvedUnion
import drift.types.VoidType
import drift.types.resolve
import drift.values.ParserPrimitiveClass
import language.LangInfo.INJECTED_VAR_PREFIX
import language.LangInfo.NAMESPACE_SEPARATOR
import language.ModuleReference
import language.Namespace


class SemanticChecker(
    val namespace: Namespace,
    val ast: List<ParserStatement>,
    val symbolTable: SymbolTable,
    val refResolutions: Map<NodeId, NodeId>,
    val resolutions: TypeInference.TypeInferenceResult) {

    val callableContextScopes: ArrayDeque<ParserCallable> = ArrayDeque()


    fun check() {
        ast.forEach { checkStatement(it) }
    }


    /* STATEMENTS */

    private fun checkStatement(statement: ParserStatement) {
        when (statement) {
            is Let      -> checkLet(statement)
            is Class    -> checkClass(statement)
            is Func     -> checkFunction(statement)
            is Return   -> checkReturn(statement)
            is Block    -> checkBlock(statement)
            is If       -> checkIf(statement)
            is For      -> checkFor(statement)
            is ExprStmt -> checkExprStmt(statement)

            else -> { /* No check needed. */ }
        }
    }

    private fun checkLet(let: Let) {
        let.annotations.forEach(this::checkAnnotation)
        checkType(let.type)

        val value = let.value

        if (value != null) {
            val expectedType = resolutions.typeResolutions[let.nodeId]
                ?: throw DTCTypeResolutionNotFoundException(value.nodeId)

            checkExpression(value)

            val compatibleTypes = compareTypesInLiteralContext(
                expectedType,
                value)

            if (!compatibleTypes)
                throw DTCUnexpectedTypeException(let.type.asString())
        }
    }
    private fun checkClass(`class`: Class) {
        `class`.annotations.forEach(this::checkAnnotation)
        `class`.fields.forEach(this::checkStatement)
        `class`.staticFields.forEach(this::checkStatement)
        `class`.methods.forEach(this::checkStatement)
        `class`.staticMethods.forEach(this::checkStatement)
    }
    private fun checkFunction(function: Func) {
        callableContextScopes.add(function)

        function.annotations.forEach(this::checkAnnotation)
        checkType(function.returnType)
        function.parameters.forEach { parameter ->
            checkType(parameter.type)

            val defaultValue = parameter.defaultValue

            if (defaultValue != null) {
                val paramDefaultValueCompatible = compareTypesInLiteralContext(
                    expectedType = parameter.type.resolve(ModuleReference.unresolved, namespace),
                    expression = defaultValue)

                if (!paramDefaultValueCompatible)
                    throw DTCUnexpectedTypeException(parameter.type.asString())
            }
        }
        function
            .body
            .statements
            .forEach(this::checkStatement)

        callableContextScopes.removeLast()
    }
    private fun checkReturn(`return`: Return) {
        if (callableContextScopes.isEmpty())
            throw DTCUnexpectedReturnStatementException()

        val funcCtx = callableContextScopes.last() as? ParserReturnable
            ?: throw DTCCannotReturnValueInNonReturnableContextException()

        val returnValue = `return`.value
        if (returnValue != null) {
            checkExpression(returnValue)

            val declaredReturnType = funcCtx.returnType
            val expectedType = resolutions.typeResolutions[funcCtx.nodeId]
                ?: if (declaredReturnType is LastType) throw DTCTypeResolutionNotFoundException(funcCtx.nodeId)
                   else declaredReturnType.resolve(ModuleReference.unresolved, namespace)

            val compatibleTypes = compareTypesInLiteralContext(
                expectedType,
                returnValue)

            if (!compatibleTypes)
                throw DTCUnexpectedTypeException(expectedType.asString())
        }
    }
    private fun checkBlock(block: Block) = block.statements.forEach { checkStatement(it) }
    private fun checkIf(`if`: If) {
        checkExpression(`if`.condition)
        checkStatement(`if`.thenBranch)
        `if`.elseBranch?.let(this::checkStatement)
    }
    private fun checkFor(`for`: For) {
        val iterable = `for`.iterable

        checkExpression(`for`.iterable)
        val iterableType = (resolutions.typeResolutions[iterable.nodeId]
            ?: throw DTCTypeResolutionNotFoundException(iterable.nodeId)) as? ObjectType
            ?: throw DTCUnsupportedIterationException()
        val iterableClassId = symbolTable.lookupNodeId(iterableType.qualifiedName.qualifiedName)
            ?: throw DTCClassNotFoundException(iterableType.qualifiedName.qualifiedName)
        val iterableClass = symbolTable.getSymbol(iterableClassId) as ClassSymbol

        if (!iterableClass.signature.methods.containsKey("iterate"))
            // TODO: replace 'iterate' check by Iterable interface impl
            throw DTCUnsupportedIterationException()

        checkStatement(`for`.body)
    }
    private fun checkExprStmt(exprStmt: ExprStmt) = checkExpression(exprStmt.expr)


    /* EXPRESSIONS */

    private fun checkExpression(expression: ParserExpression) {
        when (expression) {
            is Unary        -> checkUnary(expression)
            is Binary       -> checkBinary(expression)
            is Call         -> checkCall(expression)
            is Assign       -> checkAssign(expression)
            is Get          -> checkGet(expression)
            is Set          -> checkSet(expression)
            is Conditional  -> checkConditional(expression)
            is Lambda       -> checkLambda(expression)
            is Reference    -> checkReference(expression)
            is drift.ast.expressions.Array  -> checkListLiteral(expression)

            else -> {
                /* Undefined behavior. */
                println("[WARNING]\tUnhandled expression checking for: ${expression::class.qualifiedName}")
            }
        }
    }

    private fun checkUnary(unary: Unary) = checkExpression(unary.expr)
    private fun checkBinary(binary: Binary) {
        checkExpression(binary.left)
        checkExpression(binary.right)
    }
    private fun checkCall(call: Call) {
        val callee = call.callee
        val args = call.args


        fun checkCallableArguments(symbol: CallableSymbol) {
            val parameters = symbol.signature.parameterTypes
            val minArgsSize = parameters
                .filter { it.isRequired }
                .size
            val maxArgsSize = parameters.size

            if (args.size !in minArgsSize..maxArgsSize) {
                throw DTCInvalidArgsCountException(
                    expected = parameters.size,
                    given = args.size)
            }

            // 1. named args

            val namedArgs = args
                .filter { it.name != null }

            namedArgs.forEach { arg ->
                val parameter = parameters
                    .firstOrNull { it.name == arg.name }
                    ?: throw DTCMissingNamedParameterException(paramName = arg.name!!)

                checkExpression(arg.expr)

                val compatible = compareTypesInLiteralContext(
                    parameter.type,
                    arg.expr)

                if (!compatible)
                    throw DTCUnexpectedTypeException(parameter.type.asString())
            }


            // 2. remaining positional args

            val positionalArgs = args
                .filter { it.name == null }

            positionalArgs.onEachIndexed { index, arg ->
                val parameter = parameters
                    .getOrNull(index)
                    ?: throw DTCMissingPositionalParameterException(position = index)

                checkExpression(arg.expr)

                val compatible = compareTypesInLiteralContext(
                    parameter.type,
                    arg.expr)

                if (!compatible)
                    throw DTCUnexpectedTypeException(parameter.type.asString())
            }
        }

        fun handleReference(callee: Reference) {
            val calleeDefId = refResolutions[callee.nodeId]
                ?: throw DTCRefResolutionNotFoundException()

            when (val symbol = symbolTable.getSymbol(calleeDefId)) {
                is CallableSymbol -> checkCallableArguments(symbol)
                is ClassSymbol -> {
                    val signature = symbol.signature
                    val constructor = signature.constructorMethod

                    checkCallableArguments(constructor)
                }
            }
        }

        fun handleAccessor(callee: Get) {
            val receiverType = (resolutions.typeResolutions[callee.receiver.nodeId]
                ?: throw DTCTypeResolutionNotFoundException(callee.receiver.nodeId))

            val methodSignature: CallableSignature = when (receiverType) {
                is ObjectType -> {
                    val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                        ?: throw DTCClassNotFoundException(receiverType.qualifiedName.qualifiedName)
                    val classSymbol = symbolTable.getSymbol(classId) as? ClassSymbol
                        ?: throw DTCUnexpectedCalleeException()

                    classSymbol.signature.methods[callee.name]
                        ?: throw DTCRefResolutionNotFoundException()
                }
                is ClassType -> {
                    val classId = symbolTable.lookupNodeId(receiverType.qualifiedName.qualifiedName)
                        ?: throw DTCClassNotFoundException(receiverType.qualifiedName.qualifiedName)
                    val classSymbol = symbolTable.getSymbol(classId) as? ClassSymbol
                        ?: throw DTCUnexpectedCalleeException()

                    classSymbol.signature.staticMethods[callee.name]
                        ?: throw DTCRefResolutionNotFoundException()
                }

                else -> throw DTCUnexpectedCalleeException()
            }

            checkCallableArguments(CallableSymbol(methodSignature))
        }


        checkExpression(callee)

        when (callee) {
            is Reference -> handleReference(callee)
            is Get      -> handleAccessor(callee)

            else        -> throw DTCUnexpectedCalleeException()
        }
    }
    private fun checkAssign(assign: Assign) {
        if (assign.name.startsWith(INJECTED_VAR_PREFIX))
            error("Injected variables are immutable")

        val structureNodeId = symbolTable.lookupNodeId(assign.name)
            ?: symbolTable.lookupNodeId("$namespace$NAMESPACE_SEPARATOR${assign.name}")
            ?: error("Assign variable not found")
        val structure = symbolTable.getSymbol(structureNodeId) as? VariableSymbol
            ?: error("Only variables can be assigned")

        if (!structure.signature.isMutable)     // TODO: except N/A lets
            error("Variable '${assign.name}' is immutable")

        checkExpression(assign.value)
    }
    private fun checkGet(get: Get) = checkExpression(get.receiver)
    private fun checkSet(set: Set) {
        checkExpression(set.receiver)
        checkExpression(set.value)
    }
    private fun checkConditional(conditional: Conditional) {
        checkExpression(conditional.condition)
        checkStatement(conditional.thenBranch)
        conditional.elseBranch?.let(this::checkStatement)
    }
    private fun checkLambda(lambda: Lambda) {
        callableContextScopes.add(lambda)

        checkType(lambda.returnType)
        lambda.parameters.forEach { parameter ->
            checkType(parameter.type)

            val defaultValue = parameter.defaultValue

            if (defaultValue != null) {
                val paramDefaultValueCompatible = compareTypesInLiteralContext(
                    expectedType = parameter.type.resolve(ModuleReference.unresolved, namespace),
                    expression = defaultValue)

                if (!paramDefaultValueCompatible)
                    throw DTCUnexpectedTypeException(parameter.type.asString())
            }
        }
        lambda
            .body
            .statements
            .forEach(this::checkStatement)

        callableContextScopes.removeLast()
    }
    private fun checkReference(reference: Reference) {
        if (!refResolutions.contains(reference.nodeId))
            throw DTCRefResolutionNotFoundException()
    }
    private fun checkListLiteral(array: drift.ast.expressions.Array) {
        array.values.forEach(this::checkExpression)
    }


    /* METADATA */

    private fun checkAnnotation(annotation: Annotation) {
        annotation.args.forEach { argument ->
            checkExpression(argument.expr)
        }
    }


    /* TYPES */

    private fun compareTypesInLiteralContext(
        expectedType: Type,
        expression: ParserExpression) : Boolean {

        val resolvedType =
            if (expression is Literal) {
                when (expression.value) {
                    is drift.values.primaries.NumericValue ->
                        resolutions.typeResolutions[expression.nodeId] ?: ObjectType(ParserPrimitiveClass.Int)
                    is drift.values.primaries.IntValue -> ObjectType(ParserPrimitiveClass.Int)
                    is drift.values.primaries.Int64Value -> ObjectType(ParserPrimitiveClass.Int64)
                    is drift.values.primaries.UIntValue -> ObjectType(ParserPrimitiveClass.UInt)
                    is drift.values.primaries.BoolValue -> ObjectType(ParserPrimitiveClass.Bool)
                    is drift.values.primaries.StringValue -> ObjectType(ParserPrimitiveClass.String)
                    is drift.values.primaries.NullValue -> NullType
                }
            } else {
                resolutions.typeResolutions[expression.nodeId]
                    ?: return true      // TODO: confirm this true fallback??
            }

        return compareTypeStructures(expectedType, resolvedType)
    }

    private fun compareTypeStructures(expectedType: Type, receivedType: Type): Boolean {
        return when (expectedType) {
            is OptionalType -> {
                compareTypes(expectedType.inner, receivedType) ||
                receivedType is NullType
            }

            is UnionType -> {
                var state = false

                for (type in expectedType.options) {
                    if (compareTypes(type, receivedType))
                        state = true
                }

                state
            }

            else -> compareTypes(expectedType, receivedType)
        }
    }

    private fun compareTypes(
        expectedType: Type,
        receivedType: Type) : Boolean {

        if (receivedType is UnknownType)
            return true

        return when (expectedType) {
            is AnyType -> true
            is ObjectType -> {
                receivedType is ObjectType &&
                expectedType.qualifiedName == receivedType.qualifiedName &&
                expectedType.args == receivedType.args
            }
            is NullType -> {
                receivedType is NullType
            }
            is VoidType -> {
                receivedType is VoidType
            }
            is FunctionType -> {
                receivedType is FunctionType &&
                compareTypes(expectedType.returnType, receivedType.returnType) &&
                expectedType.paramTypes == receivedType.paramTypes
            }
            is ClassType -> {
                receivedType is ClassType &&
                expectedType.qualifiedName == receivedType.qualifiedName &&
                expectedType.generics == receivedType.generics
            }

            else -> false
        }
    }

    private fun checkType(type: UnresolvedType) {
        when (type) {
            is UnresolvedOptional -> checkType(type.inner)
            is UnresolvedUnion -> type.options.forEach { checkType(it) }
            is UnresolvedObjectType -> {
                val isKnownPrimitive = ParserPrimitiveClass.entries
                    .any { it.className == type.name }

                if (!isKnownPrimitive && !symbolTable.hasClass("$namespace$NAMESPACE_SEPARATOR${type.name}")) {
                    throw DTCClassNotFoundException(type.name)
                }
            }

            else -> { /* No check needed. */ }
        }
    }
}