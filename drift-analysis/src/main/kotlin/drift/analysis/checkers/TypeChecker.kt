/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.checkers

import drift.analysis.inference.TypeInference
import drift.analysis.symbols.CallableSymbol
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.ast.ParserCallable
import drift.ast.ParserReturnable
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.metadata.Annotation
import drift.ast.statements.*
import drift.runtime.AnyType
import drift.runtime.ClassType
import drift.runtime.FunctionType
import drift.runtime.NullType
import drift.runtime.ObjectType
import drift.runtime.OptionalType
import drift.runtime.ParserType
import drift.runtime.UnionType
import drift.runtime.UnknownType
import drift.runtime.VoidType
import drift.runtime.exceptions.DRClassNotDefinedException


class TypeChecker(
    val ast: List<ParserStatement>,
    val symbolTable: SymbolTable,
    val refResolutions: Map<Int, Int>,
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

        val compatibleTypes = compareTypesInLiteralContext(
            let.type,
            let.value)

        if (!compatibleTypes) {
            error("error type compatibility in let (expected ${let.type})")
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
                    expectedType = parameter.type,
                    expression = defaultValue)

                if (!paramDefaultValueCompatible)
                    error("error type compatibility in func param default value")
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
            error("error return outside callable context")

        val funcCtx = callableContextScopes.last() as? ParserReturnable
            ?: error("error return stmt in nno-returnable context")

        checkExpression(`return`.value)

        val compatibleTypes = compareTypesInLiteralContext(
            funcCtx.returnType,
            `return`.value)

        if (!compatibleTypes)
            error("error type compatibility in return statement")
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
            ?: error("unexpected type #${iterable.nodeId}")) as? ObjectType
            ?: error("unsupported type for iteration")
        val iterableClassId = symbolTable.lookupNodeId(iterableType.className)
            ?: error("class structure not found '${iterableType.className}'")
        val iterableClass = symbolTable.getSymbol(iterableClassId) as ClassSymbol

        if (!iterableClass.signature.methods.containsKey("iterate"))
            // TODO: replace 'iterate' check by Iterable interface impl
            error("error iterable needs Iterable impl to be decomposed")

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
            is drift.ast.expressions.Array  -> checkListLiteral(expression)

            else -> { /* Undefined behavior. */ }
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

        checkExpression(callee)

        if (callee !is Variable)
            error("unexpected callee")

        val calleeDefId = refResolutions[callee.nodeId]
            ?: error("error undefined callee")

        fun checkCallableArguments(symbol: CallableSymbol) {
            val parameterTypes = symbol.signature.parameterTypes
            val minArgsSize = parameterTypes
                .filter { it.isRequired }
                .size
            val maxArgsSize = parameterTypes.size

            if (args.size !in minArgsSize..maxArgsSize)
                error("error call must have ${parameterTypes.size} args, " +
                      "not ${args.size}")

            parameterTypes.onEachIndexed { index, ctx ->
                val arg = args[index]

                checkExpression(arg.expr)

                val compatible = compareTypesInLiteralContext(
                    ctx.type,
                    arg.expr)

                if (!compatible)
                    error("error unexpected type, needed ${ctx.type}")
            }
        }

        when (val symbol = symbolTable.getSymbol(calleeDefId)) {
            is CallableSymbol -> checkCallableArguments(symbol)
            is ClassSymbol -> {
                val signature = symbol.signature
                val constructor = signature.constructorMethod

                checkCallableArguments(constructor)
            }

            else -> { }
        }
    }
    private fun checkAssign(assign: Assign) = checkExpression(assign.value)
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
                    expectedType = parameter.type,
                    expression = defaultValue)

                if (!paramDefaultValueCompatible)
                    error("error type compatibility in lambda param default value")
            }
        }
        lambda
            .body
            .statements
            .forEach(this::checkStatement)

        callableContextScopes.removeLast()
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
        expectedType: ParserType,
        expression: ParserExpression) : Boolean {

        if (expression is Literal) {
            val t = expression.value.type()

            return when (expectedType) {
                is OptionalType -> {
                    compareTypes(expectedType.inner, t) ||
                    t is NullType
                }
                is UnionType -> {
                    var state = false

                    for (type in expectedType.options) {
                        if (compareTypes(type, t))
                            state = true
                    }

                    state
                }

                else -> compareTypes(expectedType, t)
            }
        }

        return true
    }

    private fun compareTypes(
        expectedType: ParserType,
        receivedType: ParserType) : Boolean {

        if (receivedType is UnknownType)
            return true

        return when (expectedType) {
            is AnyType -> true
            is ObjectType -> {
                receivedType is ObjectType &&
                expectedType.className == receivedType.className &&
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
                expectedType.className == receivedType.className &&
                expectedType.generics == receivedType.generics
            }

            else -> false
        }
    }

    private fun checkType(type: ParserType) {
        when (type) {
            is OptionalType -> checkType(type.inner)
            is UnionType -> type.options.forEach { checkType(it) }
            is ObjectType -> if (!symbolTable.hasClass(type.className)) {
                throw DRClassNotDefinedException(name = type.className)
            }

            else -> { /* No check needed. */ }
        }
    }
}