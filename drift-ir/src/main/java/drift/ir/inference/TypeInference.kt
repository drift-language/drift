/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.inference

import drift.ast.expressions.Assign
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.Conditional
import drift.ast.expressions.Get
import drift.ast.expressions.Lambda
import drift.ast.expressions.Literal
import drift.ast.expressions.ParserExpression
import drift.ast.expressions.Set
import drift.ast.expressions.Unary
import drift.ast.expressions.Variable
import drift.ast.statements.Block
import drift.ast.statements.Class
import drift.ast.statements.ExprStmt
import drift.ast.statements.For
import drift.ast.statements.Function
import drift.ast.statements.If
import drift.ast.statements.Let
import drift.ast.statements.ParserStatement
import drift.ast.statements.Return
import drift.ir.exceptions.DIRNotDefinedClassException
import drift.ir.exceptions.DIRNotDefinedSymbolException
import drift.ir.exceptions.DIRNotDefinedVariableException
import drift.ir.exceptions.DIRUnexpectedExpressionException
import drift.ir.exceptions.DIRUnexpectedTypeException
import drift.ir.exceptions.DIRUnexpectedUnknownTypeException
import drift.ir.exceptions.DIRUnexpectedVoidTypeException
import drift.ir.exceptions.DIRUnsupportedOperationException
import drift.ir.symbols.CallableSymbol
import drift.ir.symbols.ClassSymbol
import drift.ir.symbols.SymbolTable
import drift.ir.symbols.VariableSymbol
import drift.runtime.AnyType
import drift.runtime.NullType
import drift.runtime.ObjectType
import drift.runtime.OptionalType
import drift.runtime.ParserType
import drift.runtime.UnionType
import drift.runtime.UnknownType
import drift.runtime.VoidType
import drift.runtime.isAssignable

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
            is Function -> inferFunction(statement)
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

    private fun inferFunction(function: Function) : ParserType {
        function.run {
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

            else -> UnknownType // TODO: throw?
        }
    }

    private fun inferVariable(variable: Variable) : ParserType {
        val referenceNodeId = variable.nodeId
        val definitionNodeId = refResolutions[referenceNodeId]
            ?: return UnknownType // TODO: throw

        val symbol = symbolTable.getSymbol(definitionNodeId) as VariableSymbol
        val type = typeResolutions[definitionNodeId]
            ?: symbol.typeVariable

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
                    types = Pair(exprType, null))
            }

            "!" -> {
                if (isBooleanType) exprType
                else throw DIRUnsupportedOperationException(
                    operator = unary.operator,
                    types = Pair(exprType, null))
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
                        types = Pair(leftType, rightType))
                }
            }

            "-", "*", "/", "%" -> {
                if (isNumericType) {
                    promoteNumericTypes(leftType, rightType)
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType))
                }
            }

            "<", "<=", ">", ">=" -> {
                if (isNumericType) {
                    ObjectType("Bool")
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType))
                }
            }

            "&&", "||" -> {
                if (isBooleanType) {
                    leftType
                } else {
                    throw DIRUnsupportedOperationException(
                        operator = binary.operator,
                        types = Pair(leftType, rightType))
                }
            }

            "==", "!=" -> ObjectType("Bool")

            else -> TODO("ranges, etc")
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
            ?: classRef.signature.methods[get.name]?.returnType
            ?: classRef.signature.staticMethods[get.name]?.returnType
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

    }


    data class TypeInferenceResult(
        val typeResolutions: Map<Int, ParserType>,
        val methodResolutions: Map<Int, Int>)
}