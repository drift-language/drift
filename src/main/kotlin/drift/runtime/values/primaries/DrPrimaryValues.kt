package drift.runtime.values.primaries

import drift.runtime.DrValue
import drift.runtime.ObjectType


data class DrString(val value: String) : DrValue {
    override fun asString() = value

    override fun type() = ObjectType("String")
}

data class DrInt(val value: Int) : DrValue {
    override fun asString() = value.toString()

    override fun type() = ObjectType("Int")
}

data class DrBool(val value: Boolean) : DrValue {
    override fun asString() = value.toString()

    override fun type() = ObjectType("Bool")
}