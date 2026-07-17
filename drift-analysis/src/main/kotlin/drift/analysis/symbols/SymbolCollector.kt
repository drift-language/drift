/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.analysis.symbols.CallableSymbol.CallableSignature
import drift.analysis.symbols.ClassSymbol.ClassSignature
import drift.analysis.symbols.ModuleSymbol.ModuleSignature
import drift.analysis.symbols.VariableSymbol.VariableSignature
import drift.analysis.symbols.VariableSymbol.VariableSignature.LocalScope
import drift.analysis.symbols.VariableSymbol.VariableSignature.TopLevelScope
import drift.ast.expressions.*
import drift.ast.expressions.Set
import drift.ast.statements.*
import drift.oldruntime.AnyType
import drift.oldruntime.ObjectType
import drift.oldruntime.ParserType
import drift.oldruntime.VoidType
import language.InjectedVariableUtils.injectedThis
import language.LangInfo.NAMESPACE_SEPARATOR
import language.Namespace
import language.QualifiedName
import kotlin.collections.Set as KtSet


/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
class SymbolCollector(
    val namespace: Namespace,
    val symbolTable: SymbolTable,
    val ast: List<ParserStatement>) {

    /**
     * This map links a definition node ID with a resolution node ID.
     */
    private val refResolutions = mutableMapOf<Int, Int>()

    /**
     * A closure captures variables for a callable, like a lambda or a
     * nested/top-level function. It records outer variables used in its body.
     */
    private val closures = mutableMapOf<Int, Map<String, Int>>()

    /**
     * This set contains all imported namespaces from the current [ast].
     */
    private val importedNamespaces = mutableSetOf<String>()


    /**
     *
     */
    fun collect(): CollectionResult {
        ast.forEach { collectStatement(it) }

        return CollectionResult(symbolTable, refResolutions, this@SymbolCollector.closures)
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

        val isTopLevel = symbolTable.isTopLevel()

        val scope: VariableSignature.Scope =
            if (isTopLevel) TopLevelScope(namespace)
            else LocalScope
        val signature = VariableSignature(
            type = statement.type,
            isMutable = statement.isMutable,
            scope = scope)

        val name =
            if (isTopLevel) "$namespace$NAMESPACE_SEPARATOR${statement.name}"
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
            CallableSignature.Parameter(
                name = it.name,
                type = it.type,
                isRequired = it.defaultValue == null)
        }
        val signature = CallableSignature(
            parameterTypes,
            func.returnType)
        val refsBefore = refResolutions.keys.toSet()

        symbolTable.addCallable(
            nodeId = func.nodeId,
            name = func.name,
            signature = signature)


        /* Function's Scope */

        symbolTable.scope {
            func.parameters.forEach { parameter ->
                val signature = VariableSignature(
                    type = parameter.type,
                    isMutable = false,
                    scope = LocalScope)

                symbolTable.addVariable(
                    nodeId = parameter.nodeId,
                    name = parameter.name,
                    signature = signature)      // NOTE: Callable Parameters are immutable!

                parameter.defaultValue?.let { collectExpression(it) }
            }

            if (receiverClass != null) {
                val thisSignature = VariableSignature(
                    type = ObjectType("$namespace$NAMESPACE_SEPARATOR${receiverClass.name}"),
                    isMutable = false,
                    scope = LocalScope)

                symbolTable.addVariable(
                    nodeId = symbolTable.allocateSyntheticId(),
                    name = injectedThis(),
                    signature = thisSignature)
            }

            collectBlock(func.body, newScope = false)

            closures[func.nodeId] = collectCaptures(
                entryDepth = symbolTable.currentDepth(),
                refsBefore = refsBefore)
        }
    }

    /**
     * Create a new [SymbolTable.Scope] and collect each block's statement inside it.
     */
    private fun collectBlock(block: Block, newScope: Boolean = true) {
        val collect = {
            block.statements.forEach { collectStatement(it) }
        }

        if (newScope) symbolTable.scope(collect)
        else collect()
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
            val signature = VariableSignature(
                type = AnyType,
                isMutable = false,
                scope = LocalScope)

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
        fun prepareMethods(source: List<Func>) : LinkedHashMap<String, CallableSignature> {
            return source
                .associate { method ->
                    collectFunction(method, `class`)

                    val parameterTypes = method.parameters.map {
                        CallableSignature.Parameter(
                            name = it.name,
                            type = it.type,
                            isRequired = it.defaultValue != null)
                    }
                    val signature = CallableSignature(
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
            CallableSignature.Parameter(
                name = it.name,
                type = it.type,
                isRequired = it.defaultValue == null)
        }
        val constructorSignature = CallableSignature(
            parameterTypes = ctorParameterTypes,
            returnType = VoidType)
        val constructorSymbol = CallableSymbol(constructorSignature)
        val classQualifiedName = QualifiedName(
            namespace = namespace,
            simpleName = `class`.name)
        val signature = ClassSignature(
            qualifiedName = classQualifiedName,
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

        if (import.namespace != namespace.getQualifiedName())
            importedNamespaces.add(import.namespace)

        fun handleWithWildcard() {
            val importedNodeIds = symbolTable
                .getBindingsByNamespace(import.namespace)
            val excludedImportNamespaces = mutableSetOf<String>()

            import.parts
                ?.filter { it.alias != null }
                ?.forEach { part ->
                    val qualifiedName = QualifiedName(
                        namespace = Namespace(import.namespace),
                        simpleName = part.source)
                    val nodeId = importedNodeIds[qualifiedName.qualifiedName]
                        ?: return@forEach
                    val importNewQualifiedName = QualifiedName(
                        namespace = namespace,
                        simpleName = part.alias!!)

                    symbolTable.addBinding(importNewQualifiedName, nodeId)
                    excludedImportNamespaces.add(qualifiedName.qualifiedName)
                }

            importedNodeIds
                .filter { (importedNamespace, _) -> !excludedImportNamespaces.contains(importedNamespace) }
                .forEach { (importedNamespace, importedNodeId) ->
                    val simpleName = importedNamespace
                        .substringAfterLast(NAMESPACE_SEPARATOR)
                    val qualifiedName = QualifiedName(namespace, simpleName)

                    symbolTable.addBinding(qualifiedName, importedNodeId)
                }
        }
        fun handleWithoutWildcard() {
            if (import.parts == null) return

            import.parts
                ?.forEach { part ->
                    val namespaceEnding = part.alias ?: part.source
                    val originalQualifiedName = "${import.namespace}$NAMESPACE_SEPARATOR${part.source}"
                    val qualifiedName = QualifiedName(
                        namespace = namespace,
                        simpleName = namespaceEnding)

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

            val moduleQualifiedName = QualifiedName(
                namespace = namespace,
                simpleName = import.namespace.substringAfterLast(NAMESPACE_SEPARATOR))
            val signature = ModuleSignature(
                name = moduleQualifiedName,
                symbols = importedNodeIds)

            symbolTable.addModule(
                nodeId = import.nodeId,
                signature = signature)
        }

        if (import.wildcard)            handleWithWildcard()
        else if (import.parts != null)  handleWithoutWildcard()
        else                            handleImportByAccessor()
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
     * Visits expressions to find structures that can declare symbols.
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

    /**
     * Visits the provided lambda expression, creates a scope during the
     * collection, collects its parameters and statements.
     *
     * This method also determines which outer variables it captures.
     */
    private fun collectLambda(lambda: Lambda) {
        val refsBefore = refResolutions.keys.toSet()

        symbolTable.scope {
            lambda.parameters.forEach { parameter ->
                val signature = VariableSignature(
                    type = parameter.type,
                    isMutable = false,
                    scope = LocalScope)

                symbolTable.addVariable(
                    nodeId = parameter.nodeId,
                    name = parameter.name,
                    signature = signature)
            }
            collectBlock(lambda.body, newScope = false)

            closures[lambda.nodeId] = collectCaptures(
                entryDepth = symbolTable.currentDepth(),
                refsBefore = refsBefore)
        }
    }


    /* -- CONTEXT COLLECTORS -- */

    private fun collectCaptures(entryDepth: Int, refsBefore: KtSet<Int>) : MutableMap<String, Int> {
        val newRefs = refResolutions.keys - refsBefore
        val captures = mutableMapOf<String, Int>()

        for (refNodeId in newRefs) {
            val defNodeId = refResolutions[refNodeId]
                ?: error("Unexisting definition for reference '$refNodeId'")
            val binding = symbolTable.bindingOf(defNodeId)
                ?: error("Unexisting scope depth for definition '$defNodeId'")

            symbolTable.getSymbol(defNodeId) as? VariableSymbol
                ?: continue

            val isOuter = binding.depth < entryDepth

            if (isOuter) captures[binding.simpleName] = defNodeId
        }

        return captures
    }


    /**
     *
     *
     * @author Jonathan (GitHub: belicfr)
     */
    data class CollectionResult(
        val symbolTable: SymbolTable,
        val resolutions: Map<Int, Int>,
        val closures: Map<Int, Map<String, Int>>)
}