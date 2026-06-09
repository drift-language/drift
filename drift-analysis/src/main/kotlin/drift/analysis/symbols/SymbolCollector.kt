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
import drift.oldruntime.AnyType
import drift.oldruntime.ClassType
import drift.oldruntime.ObjectType
import drift.oldruntime.ParserType
import drift.oldruntime.VoidType
import language.LangInfo
import language.LangInfo.INJECTED_VAR_PREFIX
import language.LangInfo.NAMESPACE_SEPARATOR

class SymbolCollector(
    val namespace: String,
    val symbolTable: SymbolTable,
    val statements: List<ParserStatement>) {

    private val refResolutions = mutableMapOf<Int, Int>()
    private val lambdaClosures = mutableMapOf<Int, Map<String, Int>>()

    private val importedNamespaces = mutableSetOf<String>()


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
            is Import   -> collectImport(statement)

            else        -> { /* TODO: Ignore or log? */ }
        }
    }

    /**
     * # Variable Definition Collector
     *
     * Save the variable in the [symbolTable].
     */
    private fun collectLet(statement: Let) {
        statement.value?.let(this::collectExpression)

        if (statement.type is ObjectType) {
            val className = (statement.type as ObjectType).className
            val nodeId = symbolTable.lookupNodeId(className)
                ?: symbolTable.lookupNodeId("$namespace$NAMESPACE_SEPARATOR$className")

            nodeId?.let { refResolutions[statement.nodeId] = it }
        }

        val signature = VariableSymbol.VariableSignature(
            type = statement.type,
            isMutable = statement.isMutable)

        val name =
            if (symbolTable.isTopLevel()) "$namespace$NAMESPACE_SEPARATOR${statement.name}"
            else statement.name

        symbolTable.addVariable(
            nodeId = statement.nodeId,
            name = name,
            signature = signature)
    }

    /**
     * # Function Definition Collector
     *
     * 1. Save the function in the [symbolTable]
     * 2. Open a new [SymbolTable.Scope]
     * 3. Add parameters to scope
     * 4. Inject ``$this`` variable
     * 5. Collect the function's body
     * 6. Close the scope
     *
     * @param receiverClass Class definition node, it must only be provided
     *                      for instance methods.
     */
    private fun collectFunction(func: Func, receiverClass: Class? = null) {
        val parameterTypes = func.parameters.map {
            CallableSymbol.CallableSignature.Parameter(
                name = it.name,
                type = it.type,
                isRequired = it.defaultValue == null)
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

        if (receiverClass != null) {
            val thisSignature = VariableSymbol.VariableSignature(
                type = ObjectType("$namespace$NAMESPACE_SEPARATOR${receiverClass.name}"),
                isMutable = false)

            symbolTable.addVariable(
                nodeId = symbolTable.allocateSyntheticId(),
                name = "${INJECTED_VAR_PREFIX}this",
                signature = thisSignature)
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
        `return`.value?.let(this::collectExpression)
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
                    collectFunction(method, `class`)

                    val parameterTypes = method.parameters.map {
                        CallableSymbol.CallableSignature.Parameter(
                            name = it.name,
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
            CallableSymbol.CallableSignature.Parameter(
                name = it.name,
                type = it.type,
                isRequired = it.defaultValue == null)
        }
        val constructorSignature = CallableSymbol.CallableSignature(
            parameterTypes = ctorParameterTypes,
            returnType = VoidType)
        val constructorSymbol = CallableSymbol(constructorSignature)
        val signature = ClassSymbol.ClassSignature(
            name = "$namespace$NAMESPACE_SEPARATOR${`class`.name}",
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

    private fun collectImport(import: Import) {
        if (importedNamespaces.contains(import.namespace))
            return

        if (import.namespace != namespace)
            importedNamespaces.add(import.namespace)

        fun handleWithWildcard() {
            val importedNodeIds = symbolTable
                .getBindingsByNamespace(import.namespace)
            val excludedImportNamespaces = mutableSetOf<String>()

            import.parts
                ?.filter { it.alias != null }
                ?.forEach { part ->
                    val qualifiedName = "${import.namespace}$NAMESPACE_SEPARATOR${part.source}"
                    val nodeId = importedNodeIds[qualifiedName]
                        ?: return@forEach
                    val importNewQualifiedName = "$namespace$NAMESPACE_SEPARATOR${part.alias!!}"

                    symbolTable.addBinding(importNewQualifiedName, nodeId)
                    excludedImportNamespaces.add(qualifiedName)
                }

            importedNodeIds
                .filter { (importedNamespace, _) -> !excludedImportNamespaces.contains(importedNamespace) }
                .forEach { (importedNamespace, importedNodeId) ->
                    val simpleName = importedNamespace.substringAfterLast(NAMESPACE_SEPARATOR)
                    symbolTable.addBinding("$namespace$NAMESPACE_SEPARATOR$simpleName", importedNodeId)
                }
        }
        fun handleWithoutWildcard() {
            if (import.parts == null) return

            import.parts
                ?.forEach { part ->
                    val namespaceEnding =
                        if (part.alias == null) part.source
                        else part.alias
                    val originalQualifiedName = "${import.namespace}$NAMESPACE_SEPARATOR${part.source}"
                    val qualifiedName = "${namespace}$NAMESPACE_SEPARATOR$namespaceEnding"

                    val importedNodeId = symbolTable
                        .lookupNodeId(originalQualifiedName)
                        ?: error("Undefined imported structure")

                    symbolTable.addBinding(qualifiedName, importedNodeId)
                }
        }
        fun handleImportByAccessor() {
            val importedNodeIds = symbolTable
                .getBindingsByNamespace(import.namespace)
                .map { (importedNamespace, importedNodeId) ->
                    val newNamespace = importedNamespace
                        .substringAfterLast(NAMESPACE_SEPARATOR)

                    newNamespace to importedNodeId
                }
                .toMap()

            val moduleQualifiedName =
                namespace +
                NAMESPACE_SEPARATOR +
                import.namespace.substringAfterLast(NAMESPACE_SEPARATOR)
            val signature = ModuleSymbol.ModuleSignature(
                name = moduleQualifiedName,
                symbols = importedNodeIds)

            symbolTable.addModule(
                nodeId = import.nodeId,
                signature = signature)
        }

        if (import.wildcard) handleWithWildcard()
        else if (import.parts != null) handleWithoutWildcard()
        else handleImportByAccessor()
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
                val nodeId = symbolTable.lookupNodeId(expression.name)
                    ?: symbolTable.lookupNodeId("$namespace$NAMESPACE_SEPARATOR${expression.name}")

                nodeId?.let { refResolutions[expression.nodeId] = it }

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

            is Reference -> {
                val nodeId = symbolTable.lookupNodeId(expression.name)
                    ?: symbolTable.lookupNodeId("$namespace$NAMESPACE_SEPARATOR${expression.name}")

                nodeId?.let { refResolutions[expression.nodeId] = it }
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
            val defId = symbolTable.lookupNodeId(varName)
                ?: symbolTable.lookupNodeId("$namespace$NAMESPACE_SEPARATOR$varName")
                ?: continue

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
        expression: ParserExpression?,
        names: MutableSet<String>) {

        when (expression) {
            is Reference -> names.add(expression.name)
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