package drift.runtime.values.variables

import drift.exceptions.DriftRuntimeException
import drift.runtime.DrNotAssigned
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.isAssignable


data class DrVariable(val name: String, val type: DrType, var value: DrValue, val isMutable: Boolean) : DrValue {
    override fun asString() = value.asString()
    override fun type() = value.type()

    fun set(newValue: DrValue) {
        if (!isAssignable(newValue.type(), type))
            throw DriftRuntimeException("Cannot assign ${newValue.type()} to a $type variable")

        if (value != DrNotAssigned && !isMutable)
            throw DriftRuntimeException("Cannot assign to immutable variable $name")

        value = newValue
    }
}