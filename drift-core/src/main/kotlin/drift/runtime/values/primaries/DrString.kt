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
 * DRIFT STRING RUNTIME TYPE
 *
 * Runtime class for String type.
 ******************************************************************************/



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