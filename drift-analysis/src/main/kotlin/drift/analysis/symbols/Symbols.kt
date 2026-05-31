/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.runtime.AnyType
import drift.runtime.ParserType


abstract class Symbol


data class VariableSymbol(
    val signature: VariableSignature) : Symbol() {

    data class VariableSignature(
        val type: ParserType,
        val isMutable: Boolean)
}


data class CallableSymbol(
    val signature: CallableSignature = CallableSignature()) : Symbol() {

    data class CallableSignature(
        val parameterTypes: List<Parameter> = emptyList(),
        val returnType: ParserType = AnyType) {

        data class Parameter(
            val name: String,
            val type: ParserType,
            val isRequired: Boolean)
    }
}

data class ClassSymbol(
    val signature: ClassSignature,
    val hasPrimaryConstructor: Boolean) : Symbol() {

    data class ClassSignature(
        val name: String,
        val constructorMethod: CallableSymbol,
        val fields: LinkedHashMap<String, ParserType> = linkedMapOf(),
        val staticFields: LinkedHashMap<String, ParserType> = linkedMapOf(),
        val methods: LinkedHashMap<String, CallableSymbol.CallableSignature> = linkedMapOf(),
        val staticMethods: LinkedHashMap<String, CallableSymbol.CallableSignature> = linkedMapOf())
}