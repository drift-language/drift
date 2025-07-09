package drift.runtime.values.containers

import drift.runtime.*
import drift.runtime.values.primaries.DrInt


data class DrList(val items: List<DrValue>) : DrValue {
    override fun asString(): String =
        "[ ${items.joinToString(", ") { it.asString() }} ]"

    override fun type(): DrType {
        val types = items.map { it.type() }.toSet()

        return ObjectType("List", mapOf(
            Pair("type", SingleType(when {
                types.isEmpty() -> AnyType
                types.size == 1 -> types.first()
                else            -> UnionType(types.toList())
            })
        )))
    }
}

data class DrRange(val from: DrInt, val to: DrInt) : DrValue {
    override fun asString(): String =
        "[ ${from.value} -> ${to.value} ]"

    override fun type(): DrType =
        ObjectType("Range")
}