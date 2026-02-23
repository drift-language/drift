/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.ir.exceptions.DIRNotDefinedSymbolException
import drift.runtime.ParserType

class SymbolTable {

    // Global symbol storage - symbols persist after scope pop
    private val allSymbols = mutableMapOf<Int, Symbol>()

    // Scopes for name binding resolution
    private val scopes = mutableListOf<Scope>()


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
        type: ParserType,
        isMutable: Boolean) {

        val symbol = VariableSymbol(type, isMutable)
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

        val symbol = ClassSymbol(
            signature = signature,
            hasPrimaryConstructor = hasPrimaryConstructor)
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


    private class Scope {
        val bindings = mutableMapOf<String, Int>()
    }
}