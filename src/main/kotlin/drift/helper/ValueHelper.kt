package drift.helper

import drift.runtime.DrValue
import drift.runtime.DrVariable

fun unwrap(value: DrValue) : DrValue {
    var current = value

    while (current is DrVariable)
        current = current.value

    return current
}