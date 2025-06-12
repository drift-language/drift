package drift.runtime

import kotlin.math.exp

sealed interface DrType {
    fun asString() : String
}

object IntType : DrType {
    override fun asString(): String = "Int"
}
object StringType : DrType {
    override fun asString(): String = "String"
}
object BoolType : DrType {
    override fun asString(): String = "Bool"
}
object NullType : DrType {
    override fun asString(): String = "Null"
}

object VoidType : DrType {
    override fun asString(): String = "Void"
}

object AnyType : DrType {
    override fun asString(): String = "Any"
}

data class OptionalType(val inner: DrType) : DrType {
    override fun asString() = "${inner.asString()}?"
}

data class UnionType(val options: List<DrType>) : DrType {
    override fun asString() = options.joinToString(" | ") { it.asString() }
}

data class FunctionType(val paramTypes: List<DrType>, val returnType: DrType) : DrType {
    override fun asString() = "(${paramTypes.joinToString(", ") { it.asString() }}) -> ${returnType.asString()}"
}


fun isAssignable(valueType: DrType, expected: DrType): Boolean {
    if (expected == AnyType || valueType == expected) return true

    return when (expected) {
        is OptionalType -> valueType == NullType || isAssignable(valueType, expected.inner)
        is UnionType -> expected.options.any { isAssignable(valueType, it) }
        else -> false
    }
}