/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.imports

import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType


/******************************************************************************
 * DRIFT IMPORT MODULE TYPE
 *
 * Runtime class to represent an import module.
 ******************************************************************************/



/**
 * Import statement
 */
data class DrModule(
    val name: String,
    val symbols: Map<String, DrValue>) : DrValue {

    fun get(symbol: String): DrValue? =
        symbols[symbol]

    override fun asString(): String =
        "<module $name>"

    override fun type(): DrType =
        ObjectType("Module")
}