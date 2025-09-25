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
 * DRIFT INTEGER RUNTIME TYPE
 *
 * Runtime class for Int type.
 ******************************************************************************/



/**
 * AST representation of a 32-bit integer.
 *
 * @see DrPrimary
 */
data class DrInt(
    /** Integer value */
    override val value: Int) : DrPrimary<Int>, DrValue, DrInteger<Int> {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Int")


    override fun asInt(): Int = value

    override fun asLong(): Long = try {
        value.toLong()
    } catch (_: NumberFormatException) {
        0L
    }

    override fun asUInt(): UInt = try {
        value.toUInt()
    } catch (_: NumberFormatException) {
        0U
    }
}