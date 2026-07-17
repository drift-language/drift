/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.analysis.exceptions.DIRNotDefinedSymbolException
import drift.analysis.symbols.CallableSymbol.CallableSignature
import drift.analysis.symbols.ClassSymbol.ClassSignature
import drift.analysis.symbols.ModuleSymbol.ModuleSignature
import drift.analysis.symbols.VariableSymbol.VariableSignature
import drift.analysis.symbols.VariableSymbol.VariableSignature.LocalScope
import drift.analysis.symbols.VariableSymbol.VariableSignature.TopLevelScope
import language.LangInfo.NAMESPACE_SEPARATOR
import language.QualifiedName


/**
 *
 *
 * @param allSymbols Global symbol storage. Symbols persist after scope pop.
 *
 * @author Jonathan (GitHub: belicfr)
 */
data class SymbolTable(
    val allSymbols: MutableMap<Int, Symbol> = mutableMapOf()) {

    /**
     * Stack of living scopes. From top-level (index-0) to
     * deeper.
     *
     * As introduced above, the index-0 scope is reserved for the top-level one.
     * So it must not be ended and the collection cannot be empty. Otherwise,
     * it will throw a 'structural error' exception.
     */
    private val scopes = mutableListOf<Scope>()

    /**
     * A synthetic ID permits identifying a synthetic node
     * in the [SymbolTable].
     *
     * Synthetic IDs are negative to avoid any collision with
     * AST ones.
     */
    private var currentSyntheticId = -1


    init {
        pushScope()
        // NOTE: implicit initialization of the top-level scope. It must always
        //  exist and never be deleted.
    }


    /**
     * Appends a new scope to the stack. Necessary to handle deeper bodies, it
     * permits isolating local structures.
     */
    fun pushScope() {
        scopes.add(Scope())
    }

    /**
     * Deletes the last scope.
     */
    fun popScope() {
        if (scopes.size > 1)
            scopes.removeLast()
    }


    /**
     * @return If the current deepest scope is the top-level one. It is the case
     *         only when [scopes] size equals 1.
     */
    fun isTopLevel() : Boolean = scopes.size == 1

    /**
     * Returns the current prepared [currentSyntheticId], then decrements it.
     *
     * @return Current prepared [currentSyntheticId].
     */
    fun allocateSyntheticId() : Int = currentSyntheticId--




    /**
     * Builds a [VariableSymbol] and adds it to the current scope.
     */
    fun addVariable(
        nodeId: Int,
        name: String,
        signature: VariableSymbol.VariableSignature) {

        val symbol = VariableSymbol(signature)

        allSymbols[nodeId] = symbol

        scopes.last().bindings[name] = nodeId
    }

    /**
     * Builds a [CallableSymbol] and adds it to the current scope.
     */
    fun addCallable(
        nodeId: Int,
        name: String? = null,
        signature: CallableSignature) {     // TODO: implement QualifiedName properly.

        val symbol = CallableSymbol(signature)

        allSymbols[nodeId] = symbol

        if (name != null)
            scopes.last().bindings[name] = nodeId
    }

    /**
     * Builds a [ClassSymbol] and adds it to the current scope.
     */
    fun addClass(
        nodeId: Int,
        signature: ClassSignature,
        hasPrimaryConstructor: Boolean) {

        val symbol = ClassSymbol(signature, hasPrimaryConstructor)

        allSymbols[nodeId] = symbol

        scopes.last().bindings[signature.name] = nodeId
    }

    /**
     * Builds a [ModuleSymbol] and adds it to the top-level scope.
     *
     * A module, depending on an import, can only be stated on the top-level
     * scope.
     */
    fun addModule(
        nodeId: Int,
        signature: ModuleSignature) {

        val symbol = ModuleSymbol(signature)

        allSymbols[nodeId] = symbol

        scopes.first().bindings[signature.name] = nodeId
        // NOTE: first scope because an import statement
        //  can only be done on top-level.
    }

    /**
     * Adds or replaces a binding of the last scope. A variant using a
     * [QualifiedName] object.
     *
     * @param qualifiedName Binding qualified-name.
     * @param nodeId Bound node ID.
     */
    fun addBinding(qualifiedName: QualifiedName, nodeId: Int) {
        scopes.last().bindings[qualifiedName.qualifiedName] = nodeId
        nodeBindings[nodeId] = Binding(
            depth = scopes.size - 1,
            simpleName = qualifiedName.simpleName)
    }

    /**
     * Adds or replaces a binding of the last scope. A variant using a simple
     * name string.
     *
     * @param simpleName Binding's used simple-name.
     * @param nodeId Bound node ID.
     */
    fun addBinding(simpleName: String, nodeId: Int) {
        scopes.last().bindings[simpleName] = nodeId
        nodeBindings[nodeId] = Binding(
            depth = scopes.size - 1,
            simpleName = simpleName)
    }


    /**
     * Searches [allSymbols] using the provided [nodeId] and returns the
     * [Symbol] if existing.
     *
     * @return Found [Symbol] if existing; else NULL.
     */
    fun getSymbol(nodeId: Int) : Symbol {
        return allSymbols[nodeId]
            ?: throw DIRNotDefinedSymbolException("nodeId#$nodeId")
        /*
                REVIEW: Should not be better to return NULL if the symbol is not found,
                        instead of throwing an exception. Should not throwing be the
                        responsibility of the caller?
         */
    }

    /**
     * Access to the main scope's bindings and return its
     * binding map, filtered by the provided namespace.
     *
     * @param namespace Namespace used to filter the binding map.
     * @return Binding map (qualified name: node ID) composed of all structures
     *         related to the provided namespace.
     */
    fun getBindingsByNamespace(namespace: String) : Map<String, Int> {
        if (scopes.isEmpty())
            error("None active scope, structural error")

        return scopes
            .first()
            .bindings
            .filter { it.key.startsWith("$namespace$NAMESPACE_SEPARATOR") }
    }


    /**
     * Add or replace a binding of the last scope.
     *
     * @param name Binding name, to prefix with namespace if top-level.
     * @param nodeId Bound node ID
     */
    fun addBinding(name: String, nodeId: Int) {
        scopes.last().bindings[name] = nodeId
    }


    fun lookupNodeId(name: String): Int? {
        for (scope in scopes.asReversed())
            scope.bindings[name]?.let { return it }

        return null
    }


    /**
     * Looks up for a class with the provided name and returns if it exists.
     *
     * @return If a class having the provided name is declared.
     * @see lookupNodeId
     */
    fun hasClass(name: String) : Boolean {
        val nodeId = lookupNodeId(name) 
            ?: return false

        return getSymbol(nodeId) is ClassSymbol
    }


    /**
     * '+=' operation between two [SymbolTable] instances.
     *
     * This operation merges both [allSymbols] and top-level scopes bindings.
     */
    operator fun plusAssign(other: SymbolTable) {
        allSymbols += other.allSymbols

        if (scopes.isNotEmpty() && other.scopes.isNotEmpty())
            scopes.first().bindings += other.scopes.first().bindings
    }

    /**
     * '+=' operation between a [SymbolTable] instance and a collection of other
     * instances.
     *
     * This operation repeats the singular operation between the current
     * instance and each one from the provided collection.
     */
    operator fun plusAssign(others: Collection<SymbolTable>) =
        others.forEach { plusAssign(it) }

    /**
     * '+' operation between two [SymbolTable] instances.
     *
     * This operation merges both [allSymbols] and top-level scopes bindings.
     * Then it builds a new [SymbolTable] with the merged collections, and
     * returns it.
     *
     * @return Merged [SymbolTable] new instance.
     */
    operator fun plus(other: SymbolTable) : SymbolTable {
        val allSymbols = (allSymbols + other.allSymbols)
            .toMutableMap()

        val bindings: Map<String, Int> =
            if (scopes.isNotEmpty() && other.scopes.isNotEmpty()) {
                scopes.first().bindings + other.scopes.first().bindings
            } else {
                emptyMap()
            }

        val symbolTable = SymbolTable(allSymbols)
        symbolTable
            .scopes
            .first()
            .bindings += bindings

        return symbolTable
    }

    /**
     * '+' operation between a [SymbolTable] instance and a collection of other
     * instances.
     *
     * This operation repeats the singular operation between the current
     * instance and each one from the provided collection. Then it builds a new
     * [SymbolTable] instance with merges and returns it.
     *
     * @return Merged [SymbolTable] new instance.
     */
    operator fun plus(others: Collection<SymbolTable>) : SymbolTable {
        val finalSymbolTable = this

        for (currentST in others)
            finalSymbolTable += currentST

        return finalSymbolTable
    }



    /**
     * Since a programming language supports branches, it needs to handle
     * scopes. A scope is an isolated object containing bindings between
     * structures' names and node IDs defined in it.
     */
    private class Scope {

        /**
         * Bindings between defined structures' names and node IDs.
         */
        val bindings = mutableMapOf<String, Int>()
    }
}