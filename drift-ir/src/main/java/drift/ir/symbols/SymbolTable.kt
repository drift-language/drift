/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.symbols

import drift.ir.exceptions.DIRNotDefinedSymbolException
import drift.ir.exceptions.DIRNotDefinedVariableException
import drift.runtime.ParserType

class SymbolTable {

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

        scopes.last().apply {
            symbols[nodeId] = VariableSymbol(type, isMutable)
            bindings[name] = nodeId
        }
    }

    fun addCallable(
        nodeId: Int,
        name: String? = null,
        signature: CallableSymbol.CallableSignature) {

        scopes.last().apply {
            symbols[nodeId] = CallableSymbol(signature)

            if (name != null) bindings[name] = nodeId
        }
    }

    fun addClass(
        nodeId: Int,
        signature: ClassSymbol.ClassSignature,
        hasPrimaryConstructor: Boolean) {

        scopes.last().apply {
            symbols[nodeId] = ClassSymbol(
                signature = signature,
                hasPrimaryConstructor = hasPrimaryConstructor)

            bindings[signature.name] = nodeId
        }
    }


    fun getSymbol(nodeId: Int) : Symbol {
        for (scope in scopes.asReversed())
            scope.symbols[nodeId]?.let { return it }

        throw DIRNotDefinedSymbolException("nodeId#$nodeId")
    }


    fun lookupNodeId(name: String): Int? {
        for (scope in scopes.asReversed())
            scope.bindings[name]?.let { return it }

        return null
    }


    private class Scope {

        val symbols = mutableMapOf<Int, Symbol>()
        val bindings = mutableMapOf<String, Int>()
    }
}