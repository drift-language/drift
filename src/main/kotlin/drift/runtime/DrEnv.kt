package drift.runtime

class DrEnv(
    private val parent: DrEnv? = null,
    private val values: MutableMap<String, DrValue> = mutableMapOf()) {

    fun define(name: String, value: DrValue) {
        values[name] = value
    }

    fun get(name: String) : DrValue = values[name]
        ?: parent?.get(name)
        ?: error("Undefined symbol: $name")

    fun copy() : DrEnv = DrEnv(parent, values)
}