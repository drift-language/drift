package drift.helper

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrValue
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.specials.DrVoid
import drift.runtime.values.variables.DrVariable

fun unwrap(value: DrValue) : DrValue {
    var current = value

    while (current is DrVariable)
        current = current.value

    return current
}


fun validateValue(value: DrValue, ignoreNotAssigned: Boolean = false, ignoreVoid: Boolean = false) : DrValue {
    return when (value) {
        is DrNotAssigned ->
            if (ignoreNotAssigned) value
            else throw DriftRuntimeException("Cannot use unassigned")
        is DrVoid ->
            if (ignoreVoid) value
            else throw DriftRuntimeException("Cannot use unassigned")
        else -> value
    }
}