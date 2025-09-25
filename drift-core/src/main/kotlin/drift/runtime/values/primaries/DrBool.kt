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
 * DRIFT BOOLEAN RUNTIME TYPE
 *
 * Runtime class for Boolean type.
 ******************************************************************************/



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