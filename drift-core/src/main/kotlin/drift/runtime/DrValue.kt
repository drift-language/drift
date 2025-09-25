/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime


/******************************************************************************
 * DRIFT VALUES
 *
 * Core runtime values and type container interfaces.
 ******************************************************************************/



/**
 * This interface represents the whole types existing
 * natively in Drift, like primary ones, classes,
 * callables, etc.
 */
interface DrValue {
    /** @return A prepared string version of the value */
    fun asString() : String

    /** @return The type corresponding to the value */
    fun type() : DrType
}



/**
 * This interface represent the two type containers:
 * - [SingleType]: contains once type
 * - [MultiTypes]: contains many types
 *
 * It should be only used if the structure requires
 * both single and multiple versions.
 *
 * @see SingleType
 * @see MultiTypes
 */
interface TypeArgument


/**
 * This data class permits to contain one type,
 * a powerful component of [TypeArgument] interface.
 */
data class SingleType(val type: DrType) : TypeArgument


/**
 * This data class permits to contain many types,
 * a powerful component of [TypeArgument] interface.
 */
data class MultiTypes(val types: List<DrType>) : TypeArgument