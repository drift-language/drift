package drift.runtime

import kotlin.math.exp

sealed interface DrType {
    fun asString() : String
}

data object NullType : DrType {
    override fun asString(): String = "Null"
}
data object VoidType : DrType {
    override fun asString(): String = "Void"
}
data object AnyType : DrType {
    override fun asString(): String = "Any"
}
data object LastType : DrType {
    override fun asString(): String = "Last"
}
data object UnknownType : DrType {
    override fun asString(): String = "Unknown"
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

data class ObjectType(val className: String) : DrType {
    override fun asString() = className
}

data class ClassType(val name: String) : DrType {
    override fun asString(): String = name
}


fun isAssignable(valueType: DrType, expected: DrType): Boolean {
    if (valueType == UnknownType || expected == AnyType)
        return true

    if (expected is ObjectType && valueType is ObjectType)
        return expected.className == valueType.className

    return when (expected) {
        is OptionalType -> valueType == NullType || isAssignable(valueType, expected.inner)
        is UnionType -> expected.options.any { isAssignable(valueType, it) }
        is ClassType -> valueType is ObjectType && expected.name == valueType.className
        else -> false
    }
}