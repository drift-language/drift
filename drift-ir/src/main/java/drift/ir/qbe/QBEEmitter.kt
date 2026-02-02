/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.qbe

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.ast.statements.Function
import drift.ir.exceptions.*
import drift.qbe.memory.NameRegister
import drift.qbe.structure.*
import drift.runtime.*
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserNull
import drift.runtime.values.specials.ParserVoid
import kotlin.collections.emptyList

class QBEEmitter {

    private val types = mutableListOf<QBETypeDefinition>()
    private val data = mutableListOf<QBEData>()
    private val functions = mutableListOf<QBEFunction>()

    private val register = NameRegister()

    private val scopes = ArrayDeque<MutableMap<String, Int>>()


    fun emit(ast: List<ParserStatement>) : QBEModule {
        val instructions = mutableListOf<QBEInstruction>()

        pushScope()

        ast.forEach { statement ->
            instructions.addAll(emitStatement(statement))
        }

        return QBEModule(types, data, functions)
    }


    /* -- SCOPES MANAGEMENT -- */

    private fun pushScope() {
        scopes.add(mutableMapOf())
    }

    private fun popScope() {
        scopes.removeLast()
    }

    private fun declare(name: String, nodeId: Int) {
        val scope = scopes.last()
        scope[name] = nodeId
    }

    private fun resolve(name: String) : Int? {
        return scopes.reversed().firstNotNullOfOrNull {
            it[name]
        }
    }


    /* -- STATEMENTS -- */

    private fun emitStatement(statement: ParserStatement) : List<QBEInstruction> {
        return when (statement) {
            is Function -> emitFunction(statement)
            is Let -> emitLet(statement)
            is If  -> emitIf(statement)
            is For -> emitFor(statement)
            is Block -> emitBlock(statement)
            is ExprStmt -> emitExprStmt(statement)

            else -> throw DIRUnexpectedStatementException()
        }
    }

    private fun emitClass(`class`: Class) : List<QBEInstruction> {

        return emptyList()
    }

    private fun emitFunction(function: Function) : List<QBEInstruction> {

        return emptyList()
    }

    private fun emitLet(let: Let) : List<QBEInstruction> {
        val instructions = mutableListOf<QBEInstruction>()

        val register = NameRegister()
        val letId = let.nodeId
        val letStruct = register.temp(letId)
        val (resultValue, exprInstructions) = emitExpression(let.value)

        instructions.addAll(exprInstructions)

        val letInstruction = QBEUnary(
            destination = letStruct,
            type = emitType(let.type),
            operation = QBEOpcode.COPY,
            operand = resultValue)

        instructions.add(letInstruction)

        declare(let.name, let.nodeId)

        return instructions
    }

    private fun emitIf(`if`: If) : List<QBEInstruction> {

    }

    private fun emitFor(`for`: For) : List<QBEInstruction> {

    }

    private fun emitBlock(block: Block) : List<QBEInstruction> {

    }

    private fun emitExprStmt(exprStmt: ExprStmt) : List<QBEInstruction> {

    }

    private fun emitReturn(`return`: Return) : List<QBEInstruction> {

    }


    /* -- EXPRESSIONS -- */

    private fun emitExpression(expression: ParserExpression) : Pair<QBEUsableValue, List<QBEInstruction>> {
        return when (expression) {
            is Assign -> emitAssign(expression)
            is Binary -> emitBinary(expression)
            is Call -> emitCall(expression)
            is Conditional -> emitConditional(expression)
            is Get -> emitGet(expression)
            is Lambda -> emitLambda(expression)
            is ListLiteral -> emitListLiteral(expression)
            is Literal -> emitLiteral(expression)
            is Set -> emitSet(expression)
            is Unary -> emitUnary(expression)
            is Variable -> emitVariable(expression)

            else -> throw DIRUnexpectedExpressionException()
        }
    }

    private fun emitAssign(assign: Assign) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitBinary(binary: Binary) : Pair<QBEUsableValue, List<QBEInstruction>> {
        val left = emitExpression(binary.left)
        val right = emitExpression(binary.right)
        val operator: QBEOpcode = when (binary.operator) {
            "+" -> QBEOpcode.ADD
            "-" -> QBEOpcode.SUB
            "*" -> QBEOpcode.MUL
            "/" -> QBEOpcode.DIV

            else -> TODO()
        }

        val destination = register.temp(binary.nodeId)

        val obj = QBEBinary(
            destination,
            )
    }

    private fun emitCall(call: Call) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitConditional(conditional: Conditional) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitGet(get: Get) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitLambda(lambda: Lambda) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitListLiteral(list: ListLiteral) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitLiteral(literal: Literal) : Pair<QBEUsableValue, List<QBEInstruction>> {
        return when (val value = literal.value) {

            /* -- Primaries -- */
            is ParserInt -> Pair(
                QBEInteger(value.value.toLong()),
                emptyList())

            is ParserInt64 -> Pair(
                QBEInteger(value.value),
                emptyList())

            is ParserBool -> Pair(
                QBEInteger(if (value.value) 1 else 0),
                emptyList())

            is ParserString -> {
                val dataName = register.data()
                val dataStruct = QBEData(
                    name = dataName,
                    content = listOf(QBEDataString(value.value)))

                data.add(dataStruct)

                Pair(dataName, emptyList())
            }

            /* -- Specials -- */
            is ParserNull -> Pair(QBEInteger(0), emptyList())

            is ParserVoid,
            is ParserNotAssigned -> throw DIRUnexpectedExpressionException()

            else -> TODO()
        }
    }

    private fun emitSet(set: Set) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitUnary(unary: Unary) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitVariable(variable: Variable) : Pair<QBEUsableValue, List<QBEInstruction>> {
        val declarationId = resolve(variable.name)
            ?: throw DIRNotDefinedVariableException(name = variable.name)

        return Pair(register.temp(declarationId), emptyList())
    }


    /* -- TYPES -- */

    private fun emitType(type: ParserType) : QBEType {
        return when (type) {
            is OptionalType -> emitType(type.inner)

            is UnionType -> {
                TODO("Feature not implemented yet.")
            }

            is NullType -> QBELong

            is VoidType -> throw DIRUnexpectedVoidTypeException()

            is AnyType -> QBELong

            is UnknownType -> throw DIRUnexpectedUnknownTypeException()

            is ObjectType -> when (type.className) {
                "Int", "UInt", "Bool" -> QBEWord
                "Int64", "String" -> QBELong
                "Float" -> QBESingle
                "Double" -> QBEDouble

                else -> QBELong     // NOTE: custom class => pointer
            }

            else -> throw DIRUnexpectedTypeException()
        }
    }
}
/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.qbe

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.ast.statements.Function
import drift.ir.exceptions.*
import drift.qbe.memory.NameRegister
import drift.qbe.structure.*
import drift.runtime.*
import drift.runtime.values.containers.list.ParserList
import drift.runtime.values.primaries.ParserBool
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserNull
import drift.runtime.values.specials.ParserVoid
import kotlin.collections.emptyList

class QBEEmitter {

    private val types = mutableListOf<QBETypeDefinition>()
    private val data = mutableListOf<QBEData>()
    private val functions = mutableListOf<QBEFunction>()

    private val register = NameRegister()

    private val scopes = ArrayDeque<MutableMap<String, Int>>()


    fun emit(ast: List<ParserStatement>) : QBEModule {
        val instructions = mutableListOf<QBEInstruction>()

        pushScope()

        ast.forEach { statement ->
            instructions.addAll(emitStatement(statement))
        }

        return QBEModule(types, data, functions)
    }


    /* -- SCOPES MANAGEMENT -- */

    private fun pushScope() {
        scopes.add(mutableMapOf())
    }

    private fun popScope() {
        scopes.removeLast()
    }

    private fun declare(name: String, nodeId: Int) {
        val scope = scopes.last()
        scope[name] = nodeId
    }

    private fun resolve(name: String) : Int? {
        return scopes.reversed().firstNotNullOfOrNull {
            it[name]
        }
    }


    /* -- STATEMENTS -- */

    private fun emitStatement(statement: ParserStatement) : List<QBEInstruction> {
        return when (statement) {
            is Function -> emitFunction(statement)
            is Let -> emitLet(statement)
            is If  -> emitIf(statement)
            is For -> emitFor(statement)
            is Block -> emitBlock(statement)
            is ExprStmt -> emitExprStmt(statement)

            else -> throw DIRUnexpectedStatementException()
        }
    }

    private fun emitClass(`class`: Class) : List<QBEInstruction> {

        return emptyList()
    }

    private fun emitFunction(function: Function) : List<QBEInstruction> {

        return emptyList()
    }

    private fun emitLet(let: Let) : List<QBEInstruction> {
        val instructions = mutableListOf<QBEInstruction>()

        val register = NameRegister()
        val letId = let.nodeId
        val letStruct = register.temp(letId)
        val (resultValue, exprInstructions) = emitExpression(let.value)

        instructions.addAll(exprInstructions)

        val letInstruction = QBEUnary(
            destination = letStruct,
            type = emitType(let.type),
            operation = QBEOpcode.COPY,
            operand = resultValue)

        instructions.add(letInstruction)

        declare(let.name, let.nodeId)

        return instructions
    }

    private fun emitIf(`if`: If) : List<QBEInstruction> {

    }

    private fun emitFor(`for`: For) : List<QBEInstruction> {

    }

    private fun emitBlock(block: Block) : List<QBEInstruction> {

    }

    private fun emitExprStmt(exprStmt: ExprStmt) : List<QBEInstruction> {

    }

    private fun emitReturn(`return`: Return) : List<QBEInstruction> {

    }


    /* -- EXPRESSIONS -- */

    private fun emitExpression(expression: ParserExpression) : Pair<QBEUsableValue, List<QBEInstruction>> {
        return when (expression) {
            is Assign -> emitAssign(expression)
            is Binary -> emitBinary(expression)
            is Call -> emitCall(expression)
            is Conditional -> emitConditional(expression)
            is Get -> emitGet(expression)
            is Lambda -> emitLambda(expression)
            is ListLiteral -> emitListLiteral(expression)
            is Literal -> emitLiteral(expression)
            is Set -> emitSet(expression)
            is Unary -> emitUnary(expression)
            is Variable -> emitVariable(expression)

            else -> throw DIRUnexpectedExpressionException()
        }
    }

    private fun emitAssign(assign: Assign) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitBinary(binary: Binary) : Pair<QBEUsableValue, List<QBEInstruction>> {
        val left = emitExpression(binary.left)
        val right = emitExpression(binary.right)
        val operator: QBEOpcode = when (binary.operator) {
            "+" -> QBEOpcode.ADD
            "-" -> QBEOpcode.SUB
            "*" -> QBEOpcode.MUL
            "/" -> QBEOpcode.DIV

            else -> TODO()
        }

        val destination = register.temp(binary.nodeId)

        val obj = QBEBinary(
            destination,
            )
    }

    private fun emitCall(call: Call) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitConditional(conditional: Conditional) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitGet(get: Get) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitLambda(lambda: Lambda) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitListLiteral(list: ListLiteral) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitLiteral(literal: Literal) : Pair<QBEUsableValue, List<QBEInstruction>> {
        return when (val value = literal.value) {

            /* -- Primaries -- */
            is ParserInt -> Pair(
                QBEInteger(value.value.toLong()),
                emptyList())

            is ParserInt64 -> Pair(
                QBEInteger(value.value),
                emptyList())

            is ParserBool -> Pair(
                QBEInteger(if (value.value) 1 else 0),
                emptyList())

            is ParserString -> {
                val dataName = register.data()
                val dataStruct = QBEData(
                    name = dataName,
                    content = listOf(QBEDataString(value.value)))

                data.add(dataStruct)

                Pair(dataName, emptyList())
            }

            /* -- Specials -- */
            is ParserNull -> Pair(QBEInteger(0), emptyList())

            is ParserVoid,
            is ParserNotAssigned -> throw DIRUnexpectedExpressionException()

            else -> TODO()
        }
    }

    private fun emitSet(set: Set) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitUnary(unary: Unary) : Pair<QBEUsableValue, List<QBEInstruction>> {

    }

    private fun emitVariable(variable: Variable) : Pair<QBEUsableValue, List<QBEInstruction>> {
        val declarationId = resolve(variable.name)
            ?: throw DIRNotDefinedVariableException(name = variable.name)

        return Pair(register.temp(declarationId), emptyList())
    }


    /* -- TYPES -- */

    private fun emitType(type: ParserType) : QBEType {
        return when (type) {
            is OptionalType -> emitType(type.inner)

            is UnionType -> {
                TODO("Feature not implemented yet.")
            }

            is NullType -> QBELong

            is VoidType -> throw DIRUnexpectedVoidTypeException()

            is AnyType -> QBELong

            is UnknownType -> throw DIRUnexpectedUnknownTypeException()

            is ObjectType -> when (type.className) {
                "Int", "UInt", "Bool" -> QBEWord
                "Int64", "String" -> QBELong
                "Float" -> QBESingle
                "Double" -> QBEDouble

                else -> QBELong     // NOTE: custom class => pointer
            }

            else -> throw DIRUnexpectedTypeException()
        }
    }
}