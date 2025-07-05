package drift.runtime

import drift.ast.*
import drift.ast.Function
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException

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
    val let: Function,
    val closure: DrEnv) : DrValue {

    override fun asString(): String = "function"
    override fun type(): DrType = FunctionType(
        let.parameters.map { it.type },
        let.returnType)

    fun call(args: List<DrValue>, env: DrEnv) : DrValue {
        val local = DrEnv(parent = env)

        for ((param, arg) in let.parameters.zip(args)) {
            local.define(param.name, arg)
        }

        try {
            for (statement in let.body) {
                statement.eval(local)
            }
        } catch (e: ReturnException) {
            return e.value
        }

        return DrNull
    }
}

data class DrMethod(
    val let: Function,
    val closure: DrEnv,
    val instance: DrInstance? = null) : DrValue {

    override fun asString(): String = "<method ${let.name}>"
    override fun type(): DrType = FunctionType(
        let.parameters.map { it.type },
        let.returnType)

    fun call(args: List<DrValue>, env: DrEnv) : DrValue {
        if (instance == null)
            throw DriftRuntimeException("No instance found for ${let.name}")

        val local = DrEnv(parent = env)
        val output = mutableListOf<DrValue>()

        local.define("this", instance)

        for ((param, arg) in let.parameters.zip(args)) {
            local.define(param.name, arg)
        }

        try {
            for (statement in let.body) {
                output.add(statement.eval(local))
            }
        } catch (e: ReturnException) {
            return e.value
        }

        return output.last()
    }
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

data class DrClass(
    val name: String,
    val fields: List<FunctionParameter>,
    val methods: List<DrMethod>) : DrValue {

    override fun asString() = "<class $name>"
    override fun type(): DrType = ObjectType(name)
}

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

data object DrVoid : DrValue {
    override fun asString() = "void"

    override fun type() = VoidType
}

data object DrNull : DrValue {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}

data class DrInstance(
    val klass: DrClass,
    val values: MutableMap<String, DrValue>) : DrValue {

    override fun type(): DrType = ObjectType(klass.name)
    override fun asString() : String {
        val default = "<class ${klass.name} | instance ${this.hashCode()}>"

        return try {
            val result = callMethod("asString", emptyList())

            if (result !is DrString) {
                throw DriftTypeException("asString must return String")
            }

            result.asString()
        } catch (e: DriftRuntimeException) {
            default
        }
    }

    fun get(name: String) : DrValue {
        if (values.containsKey(name)) {
            if (klass.methods.firstOrNull { it.let.name == name } != null) {
                throw DriftRuntimeException("$name already exists")
            }

            return values[name]!!
        }

        val method = klass.methods.firstOrNull { it.let.name == name }

        if (method is DrMethod) {
            return method.copy(instance = this)
        }

        throw DriftRuntimeException("'${klass.name}.$name' property or method not found")
    }

    fun set(name: String, value: DrValue) {
        if (!values.containsKey(name))
            throw DriftRuntimeException("Cannot assign to undeclared property '${klass.name}.$name'")

        values[name] = value
    }

    private fun callMethod(name: String, args: List<DrValue>) : DrValue {
        val method = klass.methods.firstOrNull { it.let.name == name }
            ?: throw DriftRuntimeException("Method '$name' not found on class ${klass.name}")

        return method.copy(instance = this)
            .call(args, DrEnv())
    }
}

data object DrNotAssigned : DrValue {
    override fun asString(): String = UnknownType.asString()
    override fun type(): DrType = UnknownType
}

data class DrReturn(val value: DrValue) : DrValue {
    override fun asString(): String = value.asString()
    override fun type(): DrType = value.type()
}
