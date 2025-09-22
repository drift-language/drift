/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.helper

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrValue
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.specials.DrVoid
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * VALUE HELPER FUNCTIONS
 *
 * All functions which help to manipulate AST values objects
 * are defined in this file
 ******************************************************************************/



/**
 * Unwrap a AST value object
 *
 * @param value AST value object to unwrap
 * @return The contained value if exists;
 * else the provided value
 */
fun unwrap(value: DrValue) : DrValue {
    var current = value

    while (current is DrVariable)
        current = current.value

    return current
}



/**
 * Validate a AST value object
 *
 * @param value AST value object to validate
 * @param ignoreNotAssigned If NotAssigned must
 * throw an exception (cannot use unassigned)
 * @param ignoreVoid If Void must throw an exception
 * (cannot use void)
 * @return Validated value
 * @throws DriftRuntimeException If non-ignored
 * NotAssigned or Void is found (cannot use ...)
 */
fun validateValue(value: DrValue, ignoreNotAssigned: Boolean = false, ignoreVoid: Boolean = false) : DrValue {
    return when (value) {
        is DrNotAssigned ->
            if (ignoreNotAssigned) value
            else throw DriftRuntimeException("Cannot use unassigned")
        is DrVoid ->
            if (ignoreVoid) value
            else throw DriftRuntimeException("Cannot use void")
        else -> value
    }
}