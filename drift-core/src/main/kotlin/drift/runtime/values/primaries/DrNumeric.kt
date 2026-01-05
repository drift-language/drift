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
            is DrInt -> NumericRank.Int
            is DrUInt -> NumericRank.UInt
            is DrInt64 -> NumericRank.Int64
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
        NumericRank.Int64 -> Triple(DrInt64(left.asLong()), DrInt64(right.asLong()), DrInt64::class)
        NumericRank.UInt -> Triple(DrUInt(left.asUInt()), DrUInt(right.asUInt()), DrUInt::class)
        NumericRank.Int -> Triple(DrInt(left.asInt()), DrInt(right.asInt()), DrInt::class)
    }
}