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

data class SymbolTable(
    // Global symbol storage - symbols persist after scope pop
    val allSymbols: MutableMap<Int, Symbol> = mutableMapOf()) {

    // Scopes for name-binding resolution
    private val scopes: MutableList<Scope> = mutableListOf()


    init {
        pushScope()
    }


    fun pushScope() {
        scopes.add(Scope())
    }

    fun popScope() {
        if (scopes.size > 1)
            scopes.removeLast()
    }

    fun addVariable(
        nodeId: Int,
        name: String,
        signature: VariableSymbol.VariableSignature) {

        val symbol = VariableSymbol(signature)

        allSymbols[nodeId] = symbol

        scopes.last().bindings[name] = nodeId
    }

    fun addCallable(
        nodeId: Int,
        name: String? = null,
        signature: CallableSymbol.CallableSignature) {

        val symbol = CallableSymbol(signature)

        allSymbols[nodeId] = symbol

        if (name != null) scopes.last().bindings[name] = nodeId
    }

    fun addClass(
        nodeId: Int,
        signature: ClassSymbol.ClassSignature,
        hasPrimaryConstructor: Boolean) {

        val symbol = ClassSymbol(signature, hasPrimaryConstructor)

        allSymbols[nodeId] = symbol

        scopes.last().bindings[signature.name] = nodeId
    }


    fun getSymbol(nodeId: Int) : Symbol {
        return allSymbols[nodeId]
            ?: throw DIRNotDefinedSymbolException("nodeId#$nodeId")
    }


    fun lookupNodeId(name: String): Int? {
        for (scope in scopes.asReversed())
            scope.bindings[name]?.let { return it }

        return null
    }


    fun hasClass(name: String) : Boolean =
        scopes.last().bindings.containsKey(name)


    private class Scope {
        val bindings = mutableMapOf<String, Int>()
    }
}