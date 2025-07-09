/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries

import drift.runtime.DrValue
import drift.runtime.ObjectType


/******************************************************************************
 * DRIFT PRIMARY VALUE TYPES
 *
 * Primary value types compose the necessary types to code in Drift.
 ******************************************************************************/



/**
 * This interface represents all primary value types.
 */
sealed interface DrPrimary



/**
 * AST representation of a string.
 *
 * @see DrPrimary
 */
data class DrString(
    /** String value (unquoted) */
    val value: String) : DrValue, DrPrimary {


    /** @return A prepared string version of the type */
    override fun asString() = value

    /** @return The object representation of the type */
    override fun type() = ObjectType("String")
}



/**
 * AST representation of an integer.
 *
 * @see DrPrimary
 */
data class DrInt(
    /** Integer value */
    val value: Int) : DrValue, DrPrimary {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Int")
}



/**
 * AST representation of a boolean.
 *
 * @see DrPrimary
 */
data class DrBool(
    /** Boolean value */
    val value: Boolean) : DrValue, DrPrimary {



    /** @return A prepared string version of the type */
    override fun asString() = value.toString()

    /** @return The object representation of the type */
    override fun type() = ObjectType("Bool")
}