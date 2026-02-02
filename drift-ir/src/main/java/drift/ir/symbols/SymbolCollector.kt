/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.symbols

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.ast.statements.Function
import drift.runtime.AnyType

class SymbolCollector {

    private val symbolTable = SymbolTable()
    private val refResolutions = mutableMapOf<Int, Int>()


    fun collect(statements: List<ParserStatement>): CollectionResult {
        statements.forEach { collectStatement(it) }

        return CollectionResult(symbolTable, refResolutions)
    }


    /* -- STATEMENT COLLECTORS -- */

    /**
     * # Statement Collector
     *
     * Dispatch to the concerned statement collector.
     */
    private fun collectStatement(statement: ParserStatement) {
        when (statement) {
            is Let      -> collectLet(statement)
            is Function -> collectFunction(statement)
            is Block    -> collectBlock(statement)
            is If       -> collectIf(statement)
            is Return   -> collectReturn(statement)
            is For      -> collectFor(statement)
            is Class    -> collectClass(statement)
            is ExprStmt -> collectExpressionStatement(statement)

            else        -> { /* TODO: Ignore or log? */ }
        }
    }

    /**
     * # Variable Definition Collector
     *
     * Allocate a slot `S*` and save the variable in the [symbolTable].
     */
    private fun collectLet(statement: Let) : String {
        val sSlot = tempAlloc.allocate()

        collectExpression(statement.value)

        symbolTable.addVariable(
            nodeId = statement.nodeId,
            name = statement.name,
            slot = sSlot,
            type = statement.type,
            isMutable = statement.isMutable)

        return sSlot
    }

    /**
     * # Function Definition Collector
     *
     * 1. Allocate a `F*` slot for the function structure
     * 2. Save the function in the [symbolTable]
     * 3. Open a new [SymbolTable.Scope]
     * 4. Allocate `S*` slots for the parameters
     * 5. Collect the function's body
     * 6. Close the scope
     */
    private fun collectFunction(function: Function) : String {
        val fSlot = fAllocator.allocate()

        val parameterTypes = function.parameters.map { it.type }
        val signature = CallableSymbol.CallableSignature(
            parameterTypes,
            function.returnType)

        symbolTable.addCallable(
            nodeId = function.nodeId,
            name = function.name,
            slot = fSlot,
            signature = signature)


        /* Function's Scope */

        symbolTable.pushScope()

        function.parameters.forEach { parameter ->
            val paramSSlot = tempAlloc.allocate()

            symbolTable.addVariable(
                nodeId = parameter.nodeId,
                name = parameter.name,
                slot = paramSSlot,
                type = parameter.type,
                isMutable = false)      // NOTE: Callable Parameters are immutable!

            parameter.defaultValue?.let { collectExpression(it) }
        }

        function.body.forEach { collectStatement(it) }

        symbolTable.popScope()

        return fSlot
    }

    /**
     * # Block Collector
     *
     * Create a new [SymbolTable.Scope] and collect each block's statement inside it.
     */
    private fun collectBlock(block: Block) {
        symbolTable.pushScope()

        block.statements.forEach { collectStatement(it) }

        symbolTable.popScope()
    }

    /**
     * # If-Else Collector
     *
     * Collect the condition, THEN branch, and ELSE branch if existing.
     */
    private fun collectIf(`if`: If) {
        /* Condition collection */
        collectExpression(`if`.condition)

        /* THEN branch collection */
        collectStatement(`if`.thenBranch)

        /* ELSE branch collection (if exists) */
        `if`.elseBranch?.let { collectStatement(it) }
    }

    /**
     * # Return Collector
     *
     * Collect the returned expression.
     */
    private fun collectReturn(`return`: Return) {
        collectExpression(`return`.value)
    }

    /**
     * # For Collector
     *
     * Collect the iterator, the iteration variables, and the body.
     */
    private fun collectFor(`for`: For) {
        symbolTable.pushScope()

        /* Iterator collection */
        // TODO: collect iterator ? (`for`.iterable) or let it to lowerer? what to do if variable used as iterable?

        /* Iteration variables collection */
        `for`.variables.forEach { variable ->
            val variableSSlot = tempAlloc.allocate()

            symbolTable.addVariable(
                nodeId = variable.nodeId,
                name = variable.name,
                slot = variableSSlot,
                type = AnyType,
                isMutable = false)
        }

        /* Iteration body collection */
        collectStatement(`for`.body)

        symbolTable.popScope()
    }

    private fun collectClass(`class`: Class) {
        val cSlot = cAllocator.allocate()

        val fields: List<String> = `class`.fields.map { collectLet(it) }
        val staticFields: List<String> = `class`.staticFields.map { collectLet(it) }
        val methods: List<String> = `class`.methods.map { collectFunction(it) }
        val staticMethods: List<String> = `class`.staticMethods.map { collectFunction(it) }

        symbolTable.addClass(
            nodeId = `class`.nodeId,
            name = `class`.name,
            slot = cSlot,
            fields = fields,
            staticFields = staticFields,
            methods = methods,
            staticMethods = staticMethods,
            hasPrimaryConstructor = `class`.hasPrimaryConstructor)
    }

    /**
     * # Expression Statement Collector
     *
     * Collect the wrapped expression.
     */
    private fun collectExpressionStatement(exprStmt: ExprStmt) {
        collectExpression(exprStmt.expr)
    }


    /* -- EXPRESSION COLLECTORS -- */

    /**
     * # Expression Collector
     *
     * Visit expressions to find structures
     * that can declare symbols.
     */
    private fun collectExpression(expression: ParserExpression) {
        when (expression) {
            is Binary -> {
                collectExpression(expression.left)
                collectExpression(expression.right)
            }

            is Unary -> {
                collectExpression(expression.expr)
            }

            is Call -> {
                collectExpression(expression.callee)
                expression.args.forEach { collectExpression(it.expr) }
            }

            is Assign -> {
                symbolTable.lookupNodeId(expression.name)?.let {
                    refResolutions[expression.nodeId] = it
                }

                collectExpression(expression.value)
            }

            is Conditional -> {
                collectExpression(expression.condition)
                collectStatement(expression.thenBranch)

                expression.elseBranch?.let { collectStatement(it) }
                // TODO: if none else branch, collect NULL? or let this responsibility to runtime?
            }

            is Lambda -> collectLambda(expression)

            is Get -> collectExpression(expression.receiver)

            is Set -> {
                collectExpression(expression.receiver)
                collectExpression(expression.value)
            }

            is ListLiteral -> {
                expression.values.forEach { collectExpression(it) }
            }

            is Variable -> {
                symbolTable.lookupNodeId(expression.name)?.let {
                    refResolutions[expression.nodeId] = it
                }
            }

            else -> { }
        }
    }

    private fun collectLambda(lambda: Lambda) {
        val lambdaFSlot = fAllocator.allocate()
        val parameterTypes = lambda.parameters.map { it.type }
        val lambdaSignature = CallableSymbol.CallableSignature(
            parameterTypes = parameterTypes,
            returnType = lambda.returnType)

        symbolTable.addCallable(
            nodeId = lambda.nodeId,
            slot = lambdaFSlot,
            signature = lambdaSignature)

        /* Lambda's Scope */
        symbolTable.pushScope()

        lambda.parameters.forEach { parameter ->
            val paramSSlot = tempAlloc.allocate()

            symbolTable.addVariable(
                nodeId = parameter.nodeId,
                name = parameter.name,
                slot = paramSSlot,
                type = parameter.type,
                isMutable = false)
        }

        // NOTE: If in the future, Lambda can take expression as value (no block), change for collectExpression(body)?
        lambda.body.forEach { collectStatement(it) }

        symbolTable.popScope()
    }


    data class CollectionResult(
        val symbolTable: SymbolTable,
        val resolutions: Map<Int, Int>)
}