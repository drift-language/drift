/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.containers.list

import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.SingleType
import drift.runtime.UnionType


/******************************************************************************
 * DRIFT LIST RUNTIME TYPE
 *
 * Runtime class to represent List type.
 ******************************************************************************/



/**
 * Runtime List structure.
 *
 * A List is an auto-incremented index-based container.
 *
 * ### Syntax
 * Type: ```[Type]```
 * ```drift
 * let names: [String] = [ ... ]
 * ```
 */
data class DrList(
    /** List values */
    val items: List<DrValue>) : DrValue {


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "[ ${items.joinToString(", ") { it.asString() }} ]"


    /** @return The object representation of the type */
    override fun type(): DrType {
        val types = items.map { it.type() }.toSet()

        return ObjectType(
            "List", mapOf(
                Pair(
                    "type", SingleType(
                        when {
                            types.isEmpty() -> AnyType
                            types.size == 1 -> types.first()
                            else -> UnionType(types.toList())
                        }
                    )
                )
            )
        )
    }
}