/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.utils

import drift.exceptions.DriftRuntimeException
import drift.helper.unwrap
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrUInt


/******************************************************************************
 * INTEGER UTIL FUNCTIONS
 *
 * Utility functions for integers.
 ******************************************************************************/



/**
 * Concatenate two integers
 *
 * @return Result parsed in integer
 */
infix fun Int.concat(other: Int) = "$this$other".toInt()



/**
 * Cast value if numeric and needed
 *
 * @param value Value to possibly cast
 * @param expected Cast type expected
 * @throws DriftRuntimeException If the provided value is negative,
 * it cannot be cast to UInt type for safety purpose
 */
fun castNumericIfNeeded(value: DrValue, expected: DrType) : DrValue {
    if (value is DrInt) {
        return when (expected) {
            ObjectType("Int64") -> DrInt64(value.value.toLong())
            ObjectType("UInt") -> {
                val v = value.value

                if (v < 0)
                    throw DriftRuntimeException("Cannot assign negative value ($v) to UInt")

                DrUInt(v.toUInt())
            }
            else -> value
        }
    }

    return value
}