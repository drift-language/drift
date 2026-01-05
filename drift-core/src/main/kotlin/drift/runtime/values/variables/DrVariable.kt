/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.variables

import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.exceptions.DRCannotAssignToImmutableException
import drift.runtime.exceptions.DRUnassignableException
import drift.runtime.isAssignable
import drift.runtime.values.specials.DrNotAssigned


/******************************************************************************
 * DRIFT VARIABLE RUNTIME TYPE
 *
 * Runtime class for variables.
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
    override fun type() = type



    /**
     * Attempt to change the variable's value.
     *
     * @param newValue New value to apply
     */
    fun set(newValue: DrValue) {
        if (!isAssignable(newValue.type(), type))
            throw DRUnassignableException(
                newValueType = newValue.type(),
                type = type)

        if (value != DrNotAssigned && !isMutable)
            throw DRCannotAssignToImmutableException(name = name)

        value = newValue
    }
}