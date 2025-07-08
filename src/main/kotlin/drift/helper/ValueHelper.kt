package drift.helper

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrNotAssigned
import drift.runtime.DrValue
import drift.runtime.DrVariable
import drift.runtime.DrVoid

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