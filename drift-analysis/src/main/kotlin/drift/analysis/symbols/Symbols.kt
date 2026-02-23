/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.symbols

import drift.runtime.ParserType


abstract class Symbol


data class VariableSymbol(
    val typeVariable: ParserType,
    val isMutable: Boolean) : Symbol()


data class CallableSymbol(
    val signature: CallableSignature) : Symbol() {

    data class CallableSignature(
        val parameterTypes: List<ParserType>,
        val returnType: ParserType)
}

data class ClassSymbol(
    val signature: ClassSignature,
    val hasPrimaryConstructor: Boolean) : Symbol() {

    data class ClassSignature(
        val name: String,
        val fields: LinkedHashMap<String, ParserType>,
        val staticFields: LinkedHashMap<String, ParserType>,
        val methods: LinkedHashMap<String, CallableSymbol.CallableSignature>,
        val staticMethods: LinkedHashMap<String, CallableSymbol.CallableSignature>)
}