package drift.runtime.values.oop

import drift.ast.FunctionParameter
import drift.ast.eval
import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrReturn
import drift.runtime.values.primaries.DrString


data class DrClass(
    val name: String,
    val fields: List<FunctionParameter>,
    val methods: List<DrMethod>) : DrValue {

    override fun asString() = "<[class@${hashCode()}] $name>"
    override fun type(): DrType = ObjectType(name)
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
