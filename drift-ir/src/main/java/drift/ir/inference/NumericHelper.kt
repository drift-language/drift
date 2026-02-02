/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ir.inference

import drift.ir.exceptions.DIRUnexpectedTypeException
import drift.runtime.ObjectType
import drift.runtime.ParserType
import kotlin.math.max


/**
 * Numeric values ranking is necessary for
 * implicit numeric typing.
 */
enum class NumericRank(val rank: Int, val className: String) {
    Int(1, "Int"),
    UInt(2, "UInt"),
    Int64(3, "Int64");

    companion object {
        fun from(expression: String) : NumericRank? {
            return when (expression) {
                "Int" -> Int
                "UInt" -> UInt
                "Int64" -> Int64

                else -> null
            }
        }

        fun from(rank: Int) : NumericRank? {
            return when (rank) {
                Int.rank -> Int
                UInt.rank -> UInt
                Int64.rank -> Int64

                else -> null
            }
        }
    }
}

val numericClassNames = listOf<String>(
    "Int", "UInt", "Int64",
)

fun promoteNumericTypes(left: ParserType, right: ParserType) : ParserType {
    val leftClass =
        if (left is ObjectType) left.className
        else throw DIRUnexpectedTypeException()
    val rightClass =
        if (right is ObjectType) right.className
        else throw DIRUnexpectedTypeException()

    val leftNumericRank = NumericRank.from(leftClass)
        ?: throw DIRUnexpectedTypeException()
    val rightNumericRank = NumericRank.from(rightClass)
        ?: throw DIRUnexpectedTypeException()

    val maxRank = NumericRank.from(
        max(leftNumericRank.rank, rightNumericRank.rank))
        ?: throw DIRUnexpectedTypeException()

    return ObjectType(maxRank.className)
}