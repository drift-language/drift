/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.variables

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.isAssignable
import drift.runtime.values.specials.DrNotAssigned


/******************************************************************************
 * DRIFT VARIABLES STRUCTURE
 *
 * All variables are based on this file structures.
 ******************************************************************************/



/**
 * Drift variables main class.
 *
 * This class represents all variables.
 */
data class DrVariable(
    /** Variable name */
    val name: String,

    /** Variable type */
    val type: DrType,

    /** Variable value */
    var value: DrValue,

    /** If the variable can be changed */
    val isMutable: Boolean) : DrValue {



    /** @return A prepared string version of the variable */
    override fun asString() = value.asString()

    /** @return The variable's type (implicit or explicit) */
    override fun type() = value.type()



    /**
     * Attempt to change the variable's value.
     *
     * @param newValue New value to apply
     * @throws DriftRuntimeException Two cases may throw:
     * - If the new value does not respect the variable's type
     * - If the variable is immutable and already assigned
     */
    fun set(newValue: DrValue) {
        if (!isAssignable(newValue.type(), type))
            throw DriftRuntimeException("Cannot assign ${newValue.type()} to a $type variable")

        if (value != DrNotAssigned && !isMutable)
            throw DriftRuntimeException("Cannot assign to immutable variable $name")

        value = newValue
    }
}