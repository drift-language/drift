package drift.runtime

import drift.ast.DrStmt
import drift.ast.FunctionParameter

sealed interface DrValue {
    fun asString() : String

    fun type() : DrType
}

data class DrString(val value: String) : DrValue {
    override fun asString() = value

    override fun type() = StringType
}

data class DrInt(val value: Int) : DrValue {
    override fun asString() = value.toString()

    override fun type() = IntType
}

data class DrBool(val value: Boolean) : DrValue {
    override fun asString() = value.toString()

    override fun type() = BoolType
}

data class DrFunction(
    val params: List<FunctionParameter>,
    val body: List<DrStmt>,
    val closure: DrEnv,
    val returnType: DrType = AnyType) : DrValue {

    override fun asString(): String = "function"

    override fun type(): DrType = FunctionType(
        params.map { it.type },
        returnType)
}

data class DrNativeFunction(
    val impl: (List<Pair<String?, DrValue>>) -> DrValue,
    val paramTypes: List<DrType>,
    val returnType: DrType = AnyType) : DrValue {

    override fun asString(): String = "native function"

    override fun type(): DrType = FunctionType(
        paramTypes,
        returnType)
}

object DrNull : DrValue {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}


fun parseLiteral(text: String): DrValue {
    if (text.startsWith('"') && text.endsWith('"')) {
        return DrString(text.drop(1).dropLast(1))
    } else if (text == "true" || text == "false") {
        return DrBool(text == "true")
    } else if (text.toIntOrNull() != null) {
        return DrInt(text.toInt())
    } else {
        return error("Unknown type: $text")
    }
}