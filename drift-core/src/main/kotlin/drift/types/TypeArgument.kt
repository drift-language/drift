/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.types


/******************************************************************************
 * DRIFT TYPE ARGUMENTS
 *
 * Type container interfaces used to carry ObjectType's generic arguments.
 ******************************************************************************/



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
data class SingleType(val type: Type) : TypeArgument


/**
 * This data class permits containing many types,
 * a powerful component of [TypeArgument] interface.
 */
data class MultiTypes(val types: List<Type>) : TypeArgument
