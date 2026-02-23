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
import drift.exceptions.*
import drift.runtime.*

class TypeInference(
    val ast: List<ParserStatement>,
    val symbolTable: SymbolTable,
    val refResolutions: Map<Int, Int>) {

    private val typeResolutions = mutableMapOf<Int, ParserType>()
    private val methodResolutions = mutableMapOf<Int, Int>()


    fun infer() : TypeInferenceResult {
        ast.forEach { inferStatement(it) }

        return TypeInferenceResult(
            typeResolutions,
            methodResolutions)
    }


    /*  --  STATEMENTS  --  */

    private fun inferStatement(statement: ParserStatement) : ParserType {
        return when (statement) {
            is Let      -> inferLet(statement)
            is If       -> inferIf(statement)
            is Return   -> inferReturn(statement)
            is Block    -> inferBlock(statement)
            is For      -> inferFor(statement)
            is Func     -> inferFunction(statement)
            is Class    -> inferClass(statement)
            is ExprStmt -> inferExprStmt(statement)

            else -> VoidType
        }
    }

    private fun inferLet(let: Let) : ParserType {
        let.run {
            val valueType = inferExpression(value)

            val usedType =
                if (type == AnyType) valueType
                else type

            typeResolutions[nodeId] = usedType
        }

        return VoidType
    }

    private fun inferIf(`if`: If) : ParserType {
        `if`.run {
            inferExpression(condition)
            inferStatement(thenBranch)
            elseBranch?.let { inferStatement(it) }
        }

        return VoidType
    }

    private fun inferReturn(`return`: Return) : ParserType {
        inferExpression(`return`.value)

        return VoidType
    }

    private fun inferBlock(block: Block) : ParserType {
        var type: ParserType = VoidType

        block.statements.forEach { type = inferStatement(it) }

        return type
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
            body.forEach { inferStatement(it) }
        }

        return VoidType
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

    private fun inferExpression(expression: ParserExpression) : ParserType {
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
            is ListLiteral -> inferListLiteral(expression)

            else -> UnknownType // TODO: throw?
        }
    }

    private fun inferVariable(variable: Variable) : ParserType {
        val referenceNodeId = variable.nodeId
        val definitionNodeId = refResolutions[referenceNodeId]
            ?: return UnknownType // TODO: throw

        val type: ParserType = when (val symbol = symbolTable.getSymbol(definitionNodeId)) {
            is ClassSymbol -> ClassType(symbol.signature.name)
            is VariableSymbol -> {
                typeResolutions[definitionNodeId]
                    ?: symbol.typeVariable
            }

            else -> throw DIRUnexpectedExpressionException()
        }

        typeResolutions[referenceNodeId] = type

        return type
    }

    private fun inferLiteral(literal: Literal) : ParserType {
        val type = literal.value.type()
        typeResolutions[literal.nodeId] = type

        return type
    }

    private fun inferUnary(unary: Unary) : ParserType {
        val exprType = inferExpression(unary.expr)

        when (exprType) {
            is VoidType -> throw DIRUnexpectedVoidTypeException()
            is UnknownType -> throw DIRUnexpectedUnknownTypeException()

            else -> {}
        }

        val isNumericType = exprType is ObjectType &&
                            exprType.className in numericClassNames
        val isBooleanType = exprType is ObjectType &&
                            exprType.className == "Bool"

        val type: ParserType = when (unary.operator) {
            "-" -> {
                if (isNumericType) exprType
                else throw DIRUnsupportedOperationException(
                    operator = unary.operator,
                    types = Pair(exprType, null)
                )
            }

            "!" -> {
                if (isBooleanType) exprType
                else throw DIRUnsupportedOperationException(
                    operator = unary.operator,
                    types = Pair(exprType, null)
                )
            }

            else -> TODO("Unexpected operator?")
        }

        typeResolutions[unary.nodeId] = type

        return type
    }

    private fun inferBinary(binary: Binary) : ParserType {
        val leftType = inferExpression(binary.left)
        val rightType = inferExpression(binary.right)

        when {
            leftType is VoidType || rightType is VoidType ->
                throw DIRUnexpectedVoidTypeException()

            leftType is UnknownType || rightType is UnknownType ->
                throw DIRUnexpectedUnknownTypeException()
        }

        val isLeftString = leftType is ObjectType &&
                           leftType.className == "String"
        val isNumericType = leftType is ObjectType &&
                            rightType is ObjectType &&
                            leftType.className in numericClassNames &&
                            rightType.className in numericClassNames
        val isBooleanType = leftType is ObjectType &&
                            rightType is ObjectType &&
                            leftType.className == "Bool" &&
                            rightType.className == "Bool"

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
                types = Pair(leftType, rightType)
            )
        }

        typeResolutions[binary.nodeId] = type

        return type
    }

    private fun inferConditional(conditional: Conditional) : ParserType {
        val conditionType = inferExpression(conditional.condition)

        if (conditionType !is ObjectType || conditionType.className != "Bool") {
            throw DIRUnexpectedTypeException()
        }

        val thenType = inferStatement(conditional.thenBranch)
        val elseType =
            if (conditional.elseBranch != null) inferStatement(conditional.elseBranch!!)
            else NullType       // NOTE: if none else branch and condition equals FALSE,
                                //  Null is implicitly returned

        val type: ParserType = when {
            thenType == elseType -> thenType

            thenType != NullType && elseType != NullType && thenType != elseType ->
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

        call.args.forEach { inferExpression(it.expr) }

        if (callee is Variable) {
            val defId = refResolutions[callee.nodeId]
                ?: return UnknownType       // NOTE: if there isn't any ref, the structure
                                            //  isn't initialized (none ref linked to declaration)

            val type: ParserType = when (val symbol = symbolTable.getSymbol(defId)) {
                is CallableSymbol -> symbol.signature.returnType
                is ClassSymbol -> ObjectType(symbol.signature.name)
                is VariableSymbol -> {
                    val varType = typeResolutions[defId] ?: symbol.typeVariable
                    if (varType is FunctionType) varType.returnType
                    else throw DIRUnexpectedExpressionException()
                }

                else -> throw DIRUnexpectedExpressionException()
            }

            typeResolutions[call.nodeId] = type

            return type
        }

        return VoidType
    }

    private fun inferGet(get: Get) : ParserType {
        val receiverType = inferExpression(get.receiver)

        if (receiverType !is ObjectType)
            throw DIRUnexpectedTypeException()

        val classId = symbolTable.lookupNodeId(receiverType.className)
            ?: throw DIRNotDefinedClassException(name = receiverType.className)

        val classRef = symbolTable.getSymbol(classId) as ClassSymbol

        val type = classRef.signature.fields[get.name]
            ?: classRef.signature.staticFields[get.name]
            ?: classRef.signature.methods[get.name]?.let {
                FunctionType(it.parameterTypes, it.returnType)
            }
            ?: classRef.signature.staticMethods[get.name]?.let {
                FunctionType(it.parameterTypes, it.returnType)
            }
            ?: throw DIRNotDefinedSymbolException(name = get.name)

        typeResolutions[get.nodeId] = type

        return type
    }

    private fun inferSet(set: Set) : ParserType {
        val receiverType = inferExpression(set.receiver)

        if (receiverType !is ObjectType)
            throw DIRUnexpectedTypeException()

        val classId = symbolTable.lookupNodeId(receiverType.className)
            ?: throw DIRNotDefinedClassException(name = receiverType.className)

        val classRef = symbolTable.getSymbol(classId) as ClassSymbol

        val type = classRef.signature.fields[set.name]
            ?: classRef.signature.staticFields[set.name]
            ?: throw DIRNotDefinedSymbolException(name = set.name)

        val valueType = inferExpression(set.value)

        if (!isAssignable(valueType, type))
            throw DIRUnexpectedTypeException()

        typeResolutions[set.nodeId] = valueType

        return valueType
    }

    private fun inferLambda(lambda: Lambda) : ParserType {
        lambda.parameters.forEach { parameter ->
            parameter.defaultValue?.let { defValue -> inferExpression(defValue) }
        }

        val paramTypes = lambda.parameters.map { it.type }

        var lastStatementType: ParserType = AnyType

        lambda.body.forEach { lastStatementType = inferStatement(it) }

        val returnType: ParserType = when (lambda.returnType) {
            is LastType -> lastStatementType

            else -> lambda.returnType
        }

        val type = FunctionType(paramTypes, returnType)

        typeResolutions[lambda.nodeId] = type

        return type
    }

    private fun inferListLiteral(list: ListLiteral) : ParserType {
        val elementTypes = list.values
            .map { inferExpression(it) }
            .distinct()

        val listType: ParserType = when {
            elementTypes.isEmpty() -> AnyType
            elementTypes.size == 1 -> elementTypes.first()

            else -> UnionType(elementTypes)
        }

        val type = ObjectType("List", mapOf(
            "type" to SingleType(listType)))

        typeResolutions[list.nodeId] = type

        return type
    }


    data class TypeInferenceResult(
        val typeResolutions: Map<Int, ParserType>,
        val methodResolutions: Map<Int, Int>)
}