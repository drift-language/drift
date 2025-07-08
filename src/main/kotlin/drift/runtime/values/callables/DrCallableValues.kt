package drift.runtime.values.callables

import drift.ast.Function
import drift.ast.eval
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import drift.runtime.values.oop.DrInstance


data class DrFunction(
    val let: Function,
    val closure: DrEnv
) : DrValue {

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
    val returnType: DrType = AnyType
) : DrValue {

    override fun asString(): String =
        "<[native#${hashCode()}] ${type().asString()}>"

    override fun type(): DrType = FunctionType(
        paramTypes,
        returnType)
}

data class DrReturn(val value: DrValue) : DrValue {
    override fun asString(): String = value.asString()
    override fun type(): DrType = value.type()
}