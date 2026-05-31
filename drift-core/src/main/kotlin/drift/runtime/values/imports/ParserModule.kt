/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.imports

import drift.runtime.ParserType
import drift.runtime.ParserValue
import drift.runtime.ObjectType


/******************************************************************************
 * DRIFT IMPORT MODULE TYPE
 *
 * Runtime class to represent an import module.
 ******************************************************************************/



/**
 * Import statement
 */
data class ParserModule(
    val namespace: String,
    val name: String,
    val symbols: Map<String, ParserValue>) : ParserValue {

    fun get(symbol: String): ParserValue? =
        symbols[symbol]

    override fun asString(): String =
        "<module $namespace>"

    override fun type(): ParserType =
        ObjectType("Module")
}