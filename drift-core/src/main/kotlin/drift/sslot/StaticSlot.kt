/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.sslot

import drift.oldruntime.DrEnv
import drift.oldruntime.ParserType
import drift.oldruntime.ParserValue
import drift.oldruntime.exceptions.DRCannotAssignToImmutableException
import drift.oldruntime.values.variables.ParserVariable


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
    val type: ParserType,

    /** If the field is mutable */
    val isMutable: Boolean,

    /** Static field's compute lambda */
    private val initializer: (DrEnv) -> ParserValue) {


    private var variable: ParserVariable? = null


    fun get(env: DrEnv): ParserVariable {
        if (variable == null) {
            val value = initializer(env)
            variable = ParserVariable(name, type, value, isMutable)
        }

        return variable!!
    }

    fun set(env: DrEnv, value: ParserValue) {
        if (!isMutable)
            throw DRCannotAssignToImmutableException(name = name)

        val variable = get(env)
        variable.set(value)
    }
}