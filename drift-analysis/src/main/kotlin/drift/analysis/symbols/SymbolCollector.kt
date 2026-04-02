/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.runtime.AnyType
import drift.runtime.ParserType
import drift.runtime.VoidType

class SymbolCollector(
    val symbolTable: SymbolTable,
    val statements: List<ParserStatement>) {

    private val refResolutions = mutableMapOf<Int, Int>()
    private val lambdaClosures = mutableMapOf<Int, Map<String, Int>>()


    fun collect(): CollectionResult {
        statements.forEach { collectStatement(it) }

        return CollectionResult(symbolTable, refResolutions, lambdaClosures)
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
            is Func     -> collectFunction(statement)
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
     * Save the variable in the [symbolTable].
     */
    private fun collectLet(statement: Let) {
        collectExpression(statement.value)

        val signature = VariableSymbol.VariableSignature(
            type = statement.type,
            isMutable = statement.isMutable)

        symbolTable.addVariable(
            nodeId = statement.nodeId,
            name = statement.name,
            signature = signature)
    }

    /**
     * # Function Definition Collector
     *
     * 1. Save the function in the [symbolTable]
     * 2. Open a new [SymbolTable.Scope]
     * 3. Add parameters to scope
     * 4. Collect the function's body
     * 5. Close the scope
     */
    private fun collectFunction(func: Func) {
        val parameterTypes = func.parameters.map {
            CallableSymbol.CallableSignature.ParameterType(
                type = it.type,
                isRequired = it.defaultValue != null)
        }
        val signature = CallableSymbol.CallableSignature(
            parameterTypes,
            func.returnType)

        symbolTable.addCallable(
            nodeId = func.nodeId,
            name = func.name,
            signature = signature)


        /* Function's Scope */

        symbolTable.pushScope()

        func.parameters.forEach { parameter ->
            val signature = VariableSymbol.VariableSignature(
                type = parameter.type,
                isMutable = false)

            symbolTable.addVariable(
                nodeId = parameter.nodeId,
                name = parameter.name,
                signature = signature)      // NOTE: Callable Parameters are immutable!

            parameter.defaultValue?.let { collectExpression(it) }
        }

        func
            .body
            .statements
            .forEach { collectStatement(it) }

        symbolTable.popScope()
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
            val signature = VariableSymbol.VariableSignature(
                type = AnyType,
                isMutable = false)

            symbolTable.addVariable(
                nodeId = variable.nodeId,
                name = variable.name,
                signature = signature)
        }

        /* Iteration body collection */
        collectStatement(`for`.body)

        symbolTable.popScope()
    }

    private fun collectClass(`class`: Class) {
        fun prepareFields(source: List<Let>): LinkedHashMap<String, ParserType> {
            return source
                .associate { field ->
                    collectLet(field)
                    field.name to field.type
                }
                .toMap(linkedMapOf())
        }
        fun prepareMethods(source: List<Func>) : LinkedHashMap<String, CallableSymbol.CallableSignature> {
            return source
                .associate { method ->
                    collectFunction(method)

                    val parameterTypes = method.parameters.map {
                        CallableSymbol.CallableSignature.ParameterType(
                            type = it.type,
                            isRequired = it.defaultValue != null)
                    }
                    val signature = CallableSymbol.CallableSignature(
                        parameterTypes = parameterTypes,
                        returnType = method.returnType)

                    method.name to signature
                }
                .toMap(linkedMapOf())
        }

        val fields = prepareFields(`class`.fields)
        val staticFields = prepareFields(`class`.staticFields)

        val methods = prepareMethods(`class`.methods)
        val staticMethods = prepareMethods(`class`.staticMethods)
        val constructorMethod = `class`.hooks
            .first { it.name == "init" }

        val ctorParameterTypes = constructorMethod.parameters.map {
            CallableSymbol.CallableSignature.ParameterType(
                type = it.type,
                isRequired = it.defaultValue != null)
        }
        val constructorSignature = CallableSymbol.CallableSignature(
            parameterTypes = ctorParameterTypes,
            returnType = VoidType)
        val constructorSymbol = CallableSymbol(constructorSignature)
        val signature = ClassSymbol.ClassSignature(
            name = `class`.name,
            constructorMethod = constructorSymbol,
            fields = fields,
            staticFields = staticFields,
            methods = methods,
            staticMethods = staticMethods)

        symbolTable.addClass(
            nodeId = `class`.nodeId,
            signature = signature,
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

            is drift.ast.expressions.Array -> {
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
        val parameterNames = lambda.parameters.map { it.name }.toSet()

        symbolTable.pushScope()

        lambda.parameters.forEach { parameter ->
            val signature = VariableSymbol.VariableSignature(
                type = parameter.type,
                isMutable = false)

            symbolTable.addVariable(
                nodeId = parameter.nodeId,
                name = parameter.name,
                signature = signature)
        }

        lambda
            .body
            .statements
            .forEach { collectStatement(it) }

        val capturedVars = mutableMapOf<String, Int>()
        val varNamesInLambda = findVariableNamesInLambda(lambda)

        for (varName in varNamesInLambda) {
            val defId = symbolTable.lookupNodeId(varName) ?: continue

            if (!parameterNames.contains(varName)) {
                capturedVars[varName] = defId
            }
        }

        lambdaClosures[lambda.nodeId] = capturedVars

        symbolTable.popScope()
    }

    private fun findVariableNamesInLambda(lambda: Lambda): kotlin.collections.Set<String> {
        val names = mutableSetOf<String>()

        lambda.body.statements.forEach { statement ->
            collectVariableNamesInStatement(statement, names)
        }

        return names
    }

    private fun collectVariableNamesInStatement(
        statement: ParserStatement,
        names: MutableSet<String>) {

        when (statement) {
            is Let -> collectVariableNamesInExpression(statement.value, names)
            is ExprStmt -> collectVariableNamesInExpression(statement.expr, names)
            is If -> {
                collectVariableNamesInExpression(statement.condition, names)
                collectVariableNamesInStatement(statement.thenBranch, names)
                statement.elseBranch?.let { collectVariableNamesInStatement(it, names) }
            }
            is Block -> statement.statements.forEach { collectVariableNamesInStatement(it, names) }
            is Return -> collectVariableNamesInExpression(statement.value, names)
            is For -> {
                collectVariableNamesInExpression(statement.iterable, names)
                collectVariableNamesInStatement(statement.body, names)
            }
            else -> {}
        }
    }

    private fun collectVariableNamesInExpression(
        expression: ParserExpression,
        names: MutableSet<String>) {

        when (expression) {
            is Variable -> names.add(expression.name)
            is Binary -> {
                collectVariableNamesInExpression(expression.left, names)
                collectVariableNamesInExpression(expression.right, names)
            }
            is Unary -> collectVariableNamesInExpression(expression.expr, names)
            is Call -> {
                collectVariableNamesInExpression(expression.callee, names)
                for (arg in expression.args) {
                    collectVariableNamesInExpression(arg.expr, names)
                }
            }
            is Get -> collectVariableNamesInExpression(expression.receiver, names)
            is Set -> {
                collectVariableNamesInExpression(expression.receiver, names)
                collectVariableNamesInExpression(expression.value, names)
            }
            is Assign -> collectVariableNamesInExpression(expression.value, names)
            is Lambda -> {
                for (stmt in expression.body.statements) {
                    collectVariableNamesInStatement(stmt, names)
                }
            }
            is drift.ast.expressions.Array -> {
                for (value in expression.values) {
                    collectVariableNamesInExpression(value, names)
                }
            }
            else -> {}
        }
    }


    data class CollectionResult(
        val symbolTable: SymbolTable,
        val resolutions: Map<Int, Int>,
        val lambdaClosures: Map<Int, Map<String, Int>>)
}