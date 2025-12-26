/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.sslot

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * DRIFT STATIC SLOT CLASS
 *
 * Static Slot abstraction level class.
 ******************************************************************************/



/**
 * Static slot is an intermediate abstraction level
 * between AST and runtime entities for class static fields.
 *
 * It permits storing the field's value computing lambda
 * function. Useful to compute its value after the symbol collection
 * step.
 */
class StaticSlot(
    /** Static field's name */
    val name: String,

    /** Static field's type */
    val type: DrType,

    /** If the field is mutable */
    val isMutable: Boolean,

    /** Static field's compute lambda */
    private val initializer: (DrEnv) -> DrValue) {


    private var variable: DrVariable? = null


    fun get(env: DrEnv): DrVariable {
        if (variable == null) {
            val value = initializer(env)
            variable = DrVariable(name, type, value, isMutable)
        }

        return variable!!
    }

    fun set(env: DrEnv, value: DrValue) {
        if (!isMutable)
            throw DriftRuntimeException("Static field '$name' is not mutable")

        val variable = get(env)
        variable.set(value)
    }
}