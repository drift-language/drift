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
 * DRIFT INTEGER RUNTIME TYPE
 *
 * Runtime class for Int type.
 ******************************************************************************/



/**
 * Runtime representation of a 32-bits integer.
 *
 * @see DrPrimary
 */
data class ParserInt(
    /** Integer value */
    override val value: Int) : DrPrimary<Int>, ParserObject, DrInteger<Int> {

    override val className = ParserPrimitiveClass.Int.className


    override fun asInt(): Int = value

    override fun asLong(): Long =
        try {
            value.toLong()
        } catch (_: NumberFormatException) {
            0L
        }

    override fun asUInt(): UInt =
        try {
            value.toUInt()
        } catch (_: NumberFormatException) {
            0U
        }


    override fun asString() = value.toString()

    override fun type(): ParserType = ObjectType(className)
}