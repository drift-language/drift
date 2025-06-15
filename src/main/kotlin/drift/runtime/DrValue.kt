package drift.runtime

import drift.ast.Class
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

data class DrClass(val name: String, val fields: List<FunctionParameter>) : DrValue {

    override fun asString() = "<class $name>"

    override fun type(): DrType = ObjectType(name)
}

object DrVoid : DrValue {
    override fun asString() = "void"

    override fun type() = VoidType
}

object DrNull : DrValue {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}

object DrLast : DrValue {
    override fun asString() = "last"

    override fun type(): DrType = LastType
}

data class DrInstance(
    val klass: DrClass,
    val values: Map<String, DrValue>) : DrValue {

    override fun type(): DrType = ObjectType(klass.name)

    override fun asString() : String {
        val content = values.entries.joinToString(", ") { "${it.key}=${it.value.asString()}" }

        return "<${klass.name} $content>"
    }
}


fun parseLiteral(text: String): DrValue {
    if (text.startsWith('"') && text.endsWith('"')) {
        return DrString(text.drop(1).dropLast(1))
    } else if (text == "true" || text == "false") {
        return DrBool(text == "true")
    } else if (text.toIntOrNull() != null) {
        return DrInt(text.toInt())
    } else if (text == "null") {
        return DrNull
    } else {
        return error("Unknown type: $text")
    }
}