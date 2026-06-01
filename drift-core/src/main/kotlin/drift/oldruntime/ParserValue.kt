/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime


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
interface ParserValue {

    fun asString() : String

    @Deprecated("To delete with old interpreter")
    fun type() : ParserType
}



/**
 * This interface represents the two type containers:
 * - [SingleType]: contains one type
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
 * This data class permits containing one type,
 * a powerful component of [TypeArgument] interface.
 */
data class SingleType(val type: ParserType) : TypeArgument


/**
 * This data class permits containing many types,
 * a powerful component of [TypeArgument] interface.
 */
data class MultiTypes(val types: List<ParserType>) : TypeArgument