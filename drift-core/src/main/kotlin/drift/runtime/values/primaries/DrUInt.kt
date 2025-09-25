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
 * DRIFT UNSIGNED INTEGER RUNTIME TYPE
 *
 * Runtime class for Unsigned Integer type.
 ******************************************************************************/



/**
 * AST representation of an unsigned integer.
 *
 * @see DrPrimary
 */
data class DrUInt(
    /** Integer value */
    override val value: UInt) : DrPrimary<UInt>, DrValue, DrNumeric {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("UInt")


    override fun asInt(): Int = try {
        value.toInt()
    } catch (_: NumberFormatException) {
        0
    }

    override fun asLong(): Long = try {
        value.toLong()
    } catch (_: NumberFormatException) {
        0L
    }

    override fun asUInt(): UInt = value
}