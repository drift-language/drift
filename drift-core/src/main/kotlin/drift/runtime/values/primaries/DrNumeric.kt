/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries

import drift.exceptions.DriftRuntimeException
import kotlin.reflect.KClass


/******************************************************************************
 * DRIFT NUMERIC RUNTIME TYPE
 *
 * Interface for all Drift numeric types.
 ******************************************************************************/



/**
 * This interface represents all numeric value types.
 */
sealed interface DrNumeric {
    /** Get the numeric type rank for the current instance */
    val numericRank : Int
        get() = when (this) {
            is DrInt -> 1
            is DrUInt -> 2
            is DrInt64 -> 3
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
        3 -> Triple(DrInt64(left.asLong()), DrInt64(right.asLong()), DrInt64::class)
        2 -> Triple(DrUInt(left.asUInt()), DrUInt(right.asUInt()), DrUInt::class)
        1 -> Triple(DrInt(left.asInt()), DrInt(right.asInt()), DrInt::class)
        else -> throw DriftRuntimeException("Unsupported numeric promotion")
    }
}