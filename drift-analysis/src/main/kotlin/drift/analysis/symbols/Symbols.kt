/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.ast.NodeId
import drift.types.AnyType
import drift.types.Type
import language.Namespace
import language.QualifiedName


/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
abstract class Symbol {

    /**
     * A scope type permits changing how the [SymbolTable] decides whether/how
     * to bind a name.
     */
    sealed interface ScopeType

    /**
     * The local scope represents a variable declared in a non-top-level scope.
     * The variable's name should not be qualified.
     */
    data object LocalScope : ScopeType

    /**
     * The member scope represents a class member. A member's name uses its
     * class's qualified name to qualify its name.
     */
    data class MemberScope(val classQualifiedName: QualifiedName) : ScopeType

    /**
     * The top-level scope represents a structure declared outside any
     * structure, in the first and shallowest scope.
     */
    data class TopLevelScope(val namespace: Namespace) : ScopeType
}


/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
data class VariableSymbol(
    val signature: VariableSignature) : Symbol() {

    data class VariableSignature(
        val type: Type,
        val isMutable: Boolean,
        val scopeType: ScopeType)
}

/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
data class CallableSymbol(
    val signature: CallableSignature) : Symbol() {

    data class CallableSignature(
        val parameterTypes: List<Parameter> = emptyList(),
        val returnType: Type = AnyType,
        val scopeType: ScopeType) {

        data class Parameter(
            val name: String,
            val type: Type,
            val isRequired: Boolean)
    }
}

/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
data class ClassSymbol(
    val signature: ClassSignature,
    val hasPrimaryConstructor: Boolean) : Symbol() {

    data class ClassSignature(
        val qualifiedName: QualifiedName,
        val constructorMethod: CallableSymbol,
        val fields: LinkedHashMap<String, Type> = linkedMapOf(),
        val staticFields: LinkedHashMap<String, Type> = linkedMapOf(),
        val methods: LinkedHashMap<String, CallableSymbol.CallableSignature> = linkedMapOf(),
        val staticMethods: LinkedHashMap<String, CallableSymbol.CallableSignature> = linkedMapOf())
}

/**
 *
 *
 * @author Jonathan (GitHub: belicfr)
 */
data class ModuleSymbol(
    val signature: ModuleSignature) : Symbol() {

    data class ModuleSignature(
        val name: QualifiedName,
        val symbols: Map<String, NodeId> = mapOf())
}