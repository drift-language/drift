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
import drift.runtime.DrValue
import drift.runtime.ObjectType
import kotlin.reflect.KClass


/******************************************************************************
 * DRIFT PRIMARY VALUE TYPES
 *
 * Primary value types compose the necessary types to code in Drift.
 ******************************************************************************/



/**
 * This interface represents all primary value types.
 */
sealed interface DrPrimary<T> {
    val value: T
}



/**
 * This interface represents all numeric value types.
 */
sealed interface DrNumeric {
    fun asInt() : Int
    fun asLong() : Long
    fun asUInt() : UInt
}

val DrNumeric.numericRank : Int
    get() = when (this) {
        is DrInt -> 1
        is DrUInt -> 2
        is DrInt64 -> 3
        else -> 0
    }



/**
 * This interface represents all integer value types.
 */
sealed interface DrInteger<T> : DrNumeric {
    val value: T
}



/**
 * AST representation of a string.
 *
 * @see DrPrimary
 */
data class DrString(
    /** String value (unquoted) */
    override val value: String) : DrValue, DrPrimary<String> {


    /** @return A prepared string version of the type */
    override fun asString() = value

    /** @return The object representation of the type */
    override fun type() = ObjectType("String")
}



/**
 * AST representation of a 32-bit integer.
 *
 * @see DrPrimary
 */
data class DrInt(
    /** Integer value */
    override val value: Int) : DrPrimary<Int>, DrValue, DrInteger<Int> {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Int")


    override fun asInt(): Int = value

    override fun asLong(): Long = value.toLong()

    override fun asUInt(): UInt = value.toUInt()
}



/**
 * AST representation of a 64-bit integer.
 *
 * @see DrPrimary
 */
data class DrInt64(
    /** Integer value */
    override val value: Long) : DrPrimary<Long>, DrValue, DrInteger<Long> {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Int64")


    override fun asInt(): Int = value.toInt()

    override fun asLong(): Long = value

    override fun asUInt(): UInt = value.toUInt()
}



/**
 * AST representation of an unsigned integer.
 *
 * @see DrPrimary
 */
data class DrUInt(
    /** Integer value */
    override val value: UInt) : DrPrimary<UInt>, DrValue, DrNumeric {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("UInt")


    override fun asInt(): Int = value.toInt()

    override fun asLong(): Long = value.toLong()

    override fun asUInt(): UInt = value
}



/**
 * AST representation of a boolean.
 *
 * @see DrPrimary
 */
data class DrBool(
    /** Boolean value */
    override val value: Boolean) : DrValue, DrPrimary<Boolean> {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Bool")
}



/**********************************
 * NUMERIC UTILITY FUNCTIONS
 **********************************/


fun promoteNumericPair(left: DrNumeric, right: DrNumeric) : Triple<DrNumeric, DrNumeric, KClass<out DrNumeric>> {
    val rank = maxOf(left.numericRank, right.numericRank)

    return when (rank) {
        3 -> Triple(DrInt64(left.asLong()), DrInt64(right.asLong()), DrInt64::class)
        2 -> Triple(DrUInt(left.asUInt()), DrUInt(right.asUInt()), DrUInt::class)
        1 -> Triple(DrInt(left.asInt()), DrInt(right.asInt()), DrInt::class)
        else -> throw DriftRuntimeException("Unsupported numeric promotion")
    }
}