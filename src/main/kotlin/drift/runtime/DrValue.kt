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

data class DrFunction(
    val let: Function,
    val closure: DrEnv) : DrValue {

    override fun asString(): String =
        "<[function@${hashCode()}] ${let.name} ${type().asString()}>"
    override fun type(): DrType = FunctionType(
        let.parameters.map { it.type },
        let.returnType)

    fun call(args: List<DrValue>, env: DrEnv) : DrValue {
        val local = DrEnv(parent = env.copy())

        for ((param, arg) in let.parameters.zip(args)) {
            local.define(param.name, arg)
        }

        for (statement in let.body) {
            val result = statement.eval(local)

            if (result is DrReturn)
                return result.value
        }

        throw DriftRuntimeException("Missing return statement")
    }
}

data class DrLambda(
    val let: Function,
    val closure: DrEnv,
    val captures: Map<String, DrValue>) : DrValue {

    override fun asString(): String =
        "<[lambda#${hashCode()}] ${type().asString()}>"
    override fun type(): DrType = FunctionType(
        let.parameters.map { it.type },
        let.returnType)
}

data class DrMethod(
    val let: Function,
    val closure: DrEnv,
    val instance: DrValue? = null,
    val nativeImpl: DrNativeFunction? = null) : DrValue {

    override fun asString(): String {
        val i: DrInstance? = instance as? DrInstance
        var sourceString = "#"

        if (i is DrInstance) {
            sourceString = "${i.klass.name}#${i.hashCode()}"
        }

        return "<[function#${hashCode()}] $sourceString.${let.name} ${type().asString()}>"
    }
    override fun type(): DrType = FunctionType(
        let.parameters.map { it.type },
        let.returnType)

    fun call(args: List<DrValue>, env: DrEnv) : DrValue {
        if (nativeImpl != null)
            return nativeImpl.impl(instance, args.map { null to it })

        val local = DrEnv(parent = env.copy())

        if (instance == null)
            throw DriftRuntimeException("Class instance lost")

        local.define("this", instance)

        for ((param, arg) in let.parameters.zip(args)) {
            local.define(param.name, arg)
        }

        var last : DrValue

        for (statement in let.body) {
            last = statement.eval(local)

            if (last is DrReturn)
                return last.value
        }

        if (let.returnType == LastType) {

        }

        throw DriftRuntimeException("Missing return statement")
    }
}

data class DrNativeFunction(
    val name: String? = null,
    val impl: (receiver: DrValue?, args: List<Pair<String?, DrValue>>) -> DrValue,
    val paramTypes: List<DrType>,
    val returnType: DrType = AnyType) : DrValue {

    override fun asString(): String =
        "<[native#${hashCode()}] ${type().asString()}>"

    override fun type(): DrType = FunctionType(
        paramTypes,
        returnType)
}

data class DrClass(
    val name: String,
    val fields: List<FunctionParameter>,
    val methods: List<DrMethod>) : DrValue {

    override fun asString() = "<[class@${hashCode()}] $name>"
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
        val default = "<[class#${klass.hashCode()}] ${klass.name} | instance ${this.hashCode()}>"

        try {
            val method = klass.methods.firstOrNull { it.let.name == "asString" }
                ?: throw DriftRuntimeException("Method 'asString' not found on class ${klass.name}")

            val local = DrEnv()
            local.define("this", this)
            var result: DrValue? = null
            for (statement in method.let.body) {
                val evalResult = statement.eval(local)
                if (evalResult is DrReturn) {
                    result = evalResult.value
                    break
                }
                result = evalResult
            }
            if (result !is DrString) {
                throw DriftTypeException("asString must return String")
            }
            return result.asString()
        } catch (e: DriftRuntimeException) {
            return default
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
}

data object DrNotAssigned : DrValue {
    override fun asString(): String = UnknownType.asString()
    override fun type(): DrType = UnknownType
}

data class DrReturn(val value: DrValue) : DrValue {
    override fun asString(): String = value.asString()
    override fun type(): DrType = value.type()
}
