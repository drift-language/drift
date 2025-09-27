/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries

import drift.runtime.DrValue
import drift.runtime.ObjectType


/******************************************************************************
 * DRIFT 64-BITS INTEGER RUNTIME TYPE
 *
 * Runtime class for 64-bits Integer type.
 ******************************************************************************/



/**
 * AST representation of a 64-bit integer.
 *
 * @see DrPrimary
 */
data class DrInt64(
    /** Integer value */
    override val value: Long) : DrPrimary<Long>, DrValue, DrInteger<Long> {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Int64")


    override fun asInt(): Int = try {
        value.toInt()
    } catch (e: NumberFormatException) {
        0
    }

    override fun asLong(): Long = value

    override fun asUInt(): UInt = try {
        value.toUInt()
    } catch (_: NumberFormatException) {
        0U
    }
}