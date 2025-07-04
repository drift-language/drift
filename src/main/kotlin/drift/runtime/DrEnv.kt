package drift.runtime

import drift.exceptions.DriftRuntimeException

class DrEnv(
    private val parent: DrEnv? = null,
    private val values: MutableMap<String, DrValue> = mutableMapOf()) {

    fun define(name: String, value: DrValue) {
        if (values.containsKey(name))
            throw DriftRuntimeException("Variable '$name' is already defined")

        values[name] = value
    }

    fun assign(name: String, value: DrValue) {
        val variable: DrVariable = resolve(name) as? DrVariable
            ?: throw DriftRuntimeException("Variable '$name' does no longer be assigned")

        variable.set(value)
    }

    fun get(name: String) : DrValue = values[name]
        ?: parent?.get(name)
        ?: throw DriftRuntimeException("Undefined symbol: $name")

    fun resolve(name: String) : DrValue? =
        values[name] ?: parent?.resolve(name)

    fun copy() : DrEnv = DrEnv(parent, values)
}