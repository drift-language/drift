package drift.runtime.values.specials

import drift.runtime.*


data object DrVoid : DrValue {
    override fun asString() = "void"

    override fun type() = VoidType
}

data object DrNull : DrValue {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}

data object DrNotAssigned : DrValue {
    override fun asString(): String = UnknownType.asString()
    override fun type(): DrType = UnknownType
}