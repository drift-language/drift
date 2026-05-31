/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries

import drift.runtime.ParserValue
import drift.runtime.ObjectType
import drift.runtime.ParserObject
import drift.runtime.ParserPrimitiveClass
import drift.runtime.ParserType


/******************************************************************************
 * DRIFT 64-BITS INTEGER RUNTIME TYPE
 *
 * Runtime class for 64-bits Integer type.
 ******************************************************************************/



/**
 * Runtime representation of a 64-bits integer.
 *
 * @see DrPrimary
 */
data class ParserInt64(
    /** Integer value */
    override val value: Long) : DrPrimary<Long>, ParserObject, DrInteger<Long> {

    override val className = ParserPrimitiveClass.Int64.className


    override fun asInt(): Int =
        try {
            value.toInt()
        } catch (e: NumberFormatException) {
            0
        }

    override fun asLong(): Long = value

    override fun asUInt(): UInt =
        try {
            value.toUInt()
        } catch (_: NumberFormatException) {
            0U
        }


    override fun asString() = value.toString()
}