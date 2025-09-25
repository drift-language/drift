/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.containers.range

import drift.runtime.DrType
import drift.runtime.ObjectType
import drift.runtime.values.primaries.DrInteger


/******************************************************************************
 * DRIFT INCLUDED RANGE RUNTIME TYPE
 *
 * Runtime class for InclusiveRange type.
 ******************************************************************************/



/**
 * Runtime inclusive range structure.
 *
 * An inclusive range is a simplified structure representing
 * each integer between [from] and [to] (included) values.
 *
 * ### Syntax
 * Type: ```[Type..]```
 * ```
 * // This represents the values: 1, 2, 3, 4 and 5.
 * let range = 1..5
 * ```
 */
data class DrInclusiveRange(
    override val from: DrInteger<*>,
    override val to: DrInteger<*>
) : DrRange {

    companion object {
        fun factory(from: DrInteger<*>, to: DrInteger<*>) : DrRange {
            require(from::class == to::class) { "Range requires both ends to be of the same type" }

            return DrInclusiveRange(from, to)
        }
    }


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "[ ${from.value} -> ${to.value} ]"


    /** @return The object representation of the type */
    override fun type(): DrType =
        ObjectType("InclusiveRange")
}