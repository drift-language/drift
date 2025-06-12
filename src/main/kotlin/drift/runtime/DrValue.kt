package drift.runtime

import drift.ast.DrStmt
import drift.ast.FunctionParameter

sealed interface DrValue {
    fun asString(): String
}

data class DrString(val value: String) : DrValue {
    override fun asString() = value
}

data class DrInt(val value: Int) : DrValue {
    override fun asString() = value.toString()
}

data class DrBool(val value: Boolean) : DrValue {
    override fun asString() = value.toString()
}

data class DrFunction(
    val params: List<FunctionParameter>,
    val body: List<DrStmt>,
    val closure: DrEnv) : DrValue {

    override fun asString(): String = "function"
}

data class DrNativeFunction(
    val impl: (List<Pair<String?, DrValue>>) -> DrValue) : DrValue {

    override fun asString(): String = "native function"
}

object DrNull : DrValue {
    override fun asString() = "null"
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