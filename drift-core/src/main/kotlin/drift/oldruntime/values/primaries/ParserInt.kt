/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.primaries

import drift.oldruntime.ObjectType
import drift.oldruntime.ParserObject
import drift.oldruntime.ParserPrimitiveClass
import drift.oldruntime.ParserType


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