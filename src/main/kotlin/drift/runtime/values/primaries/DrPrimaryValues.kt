package drift.runtime.values.primaries

import drift.runtime.DrValue
import drift.runtime.ObjectType

sealed interface DrPrimary

data class DrString(val value: String) : DrValue, DrPrimary {
    override fun asString() = value

    override fun type() = ObjectType("String")
}

data class DrInt(val value: Int) : DrValue, DrPrimary {
    override fun asString() = value.toString()

    override fun type() = ObjectType("Int")
}

data class DrBool(val value: Boolean) : DrValue, DrPrimary {
    override fun asString() = value.toString()

    override fun type() = ObjectType("Bool")
}