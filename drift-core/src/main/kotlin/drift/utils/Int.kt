/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.utils

import drift.runtime.ParserType
import drift.runtime.ParserValue
import drift.runtime.ObjectType
import drift.runtime.exceptions.DRCannotNegateUnsignedException
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserInt64
import drift.runtime.values.primaries.ParserUInt


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
 */
fun castNumericIfNeeded(value: ParserValue, expected: ParserType) : ParserValue {
    if (value is ParserInt) {
        return when (expected) {
            ObjectType("Int64") -> ParserInt64(value.value.toLong())
            ObjectType("UInt") -> {
                val v = value.value

                if (v < 0) throw DRCannotNegateUnsignedException()

                ParserUInt(v.toUInt())
            }
            else -> value
        }
    }

    return value
}