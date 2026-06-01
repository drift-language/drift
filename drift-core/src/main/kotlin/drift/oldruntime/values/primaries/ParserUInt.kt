/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.primaries

import drift.oldruntime.ParserObject
import drift.oldruntime.ParserPrimitiveClass


/******************************************************************************
 * DRIFT UNSIGNED INTEGER RUNTIME TYPE
 *
 * Runtime class for Unsigned Integer type.
 ******************************************************************************/



/**
 * Runtime representation of a 32-bits unsigned integer.
 *
 * @see DrPrimary
 */
data class ParserUInt(
    /** Integer value */
    override val value: UInt) : DrPrimary<UInt>, ParserObject, DrNumeric {

    override val className = ParserPrimitiveClass.UInt.className


    override fun asInt(): Int =
        try {
            value.toInt()
        } catch (_: NumberFormatException) {
            0
        }

    override fun asLong(): Long =
        try {
            value.toLong()
        } catch (_: NumberFormatException) {
            0L
        }

    override fun asUInt(): UInt = value


    override fun asString() = value.toString()
}
