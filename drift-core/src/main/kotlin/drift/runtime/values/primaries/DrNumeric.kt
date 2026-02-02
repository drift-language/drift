/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries

import kotlin.reflect.KClass


/******************************************************************************
 * DRIFT NUMERIC RUNTIME TYPE
 *
 * Interface for all Drift numeric types.
 ******************************************************************************/



/**
 * Numeric values ranking is necessary for
 * implicit numeric typing.
 */
enum class NumericRank(val rank: Int) {
    Int(1),
    UInt(2),
    Int64(3)
}


/**
 * This interface represents all numeric value types.
 */
sealed interface DrNumeric {

    /** Get the numeric type rank for the current instance */
    val numericRank : NumericRank
        get() = when (this) {
            is ParserInt -> NumericRank.Int
            is ParserUInt -> NumericRank.UInt
            is ParserInt64 -> NumericRank.Int64
        }

    fun asInt() : Int
    fun asLong() : Long
    fun asUInt() : UInt
}



/**
 * Promote both values to the lower compatible numeric type.
 *
 * @param left
 * @param right
 * @return Triple of both promoted values and promotion type
 */
fun promoteNumericPair(left: DrNumeric, right: DrNumeric) : Triple<DrNumeric, DrNumeric, KClass<out DrNumeric>> {
    val rank = maxOf(left.numericRank, right.numericRank)

    return when (rank) {
        NumericRank.Int64 -> Triple(ParserInt64(left.asLong()), ParserInt64(right.asLong()), ParserInt64::class)
        NumericRank.UInt -> Triple(ParserUInt(left.asUInt()), ParserUInt(right.asUInt()), ParserUInt::class)
        NumericRank.Int -> Triple(ParserInt(left.asInt()), ParserInt(right.asInt()), ParserInt::class)
    }
}