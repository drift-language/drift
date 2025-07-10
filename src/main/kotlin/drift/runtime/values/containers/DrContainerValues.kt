/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.containers

import drift.runtime.*
import drift.runtime.values.primaries.DrInt


/******************************************************************************
 * DRIFT VALUES CONTAINMENT TYPES
 *
 * All types that can contain values are defined in this file.
 ******************************************************************************/



/**
 * Runtime list structure
 */
data class DrList(
    /** List values */
    val items: MutableList<DrValue>) : DrValue {


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "[ ${items.joinToString(", ") { it.asString() }} ]"


    /** @return The object representation of the type */
    override fun type(): DrType {
        val types = items.map { it.type() }.toSet()

        return ObjectType("List", mapOf(
            Pair("type", SingleType(when {
                types.isEmpty() -> AnyType
                types.size == 1 -> types.first()
                else            -> UnionType(types.toList())
            })
        )))
    }
}



/**
 * Runtime range structure.
 *
 * A range is a simplified structure representing
 * each integer between [from] and [to] values.
 *
 * ```
 * // This represents the values: 1, 2, 3, 4 and 5.
 * let range = 1..5
 * ```
 */
data class DrRange(
    /** From value, start of the range */
    val from: DrInt,

    /** To value, end of the range, included */
    val to: DrInt) : DrValue {


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "[ ${from.value} -> ${to.value} ]"


    /** @return The object representation of the type */
    override fun type(): DrType =
        ObjectType("Range")
}