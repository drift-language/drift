package drift.runtime

import drift.exceptions.DriftRuntimeException

class DrEnv(
    private val parent: DrEnv? = null,
    private val values: MutableMap<String, DrValue> = mutableMapOf(),
    private val classes: MutableMap<String, DrClass> = mutableMapOf()) {

    fun define(name: String, value: DrValue) {
        if (values.containsKey(name))
            throw DriftRuntimeException("Variable '$name' is already defined")

        forceDefine(name, value)
    }

    fun forceDefine(name: String, value: DrValue) {
        values[name] = value
    }

    fun defineClass(name: String, klass: DrClass) {
        if (classes.containsKey(name))
            throw DriftRuntimeException("Class '$name' is already defined")

        classes[name] = klass
    }

    fun assign(name: String, value: DrValue) {
        val variable: DrVariable = resolve(name) as? DrVariable
            ?: throw DriftRuntimeException("Variable '$name' does not exist")

        variable.set(value)
    }

    fun assignClass(name: String, klass: DrClass) {
        if (!classes.containsKey(name))
            throw DriftRuntimeException("Class '$name' does not exist")

        classes[name] = klass
    }

    fun get(name: String) : DrValue = values[name]
        ?: parent?.get(name)
        ?: throw DriftRuntimeException("Undefined symbol: $name")

    fun getClasses() : Set<String> {
        val aggregated = parent?.getClasses()?.toMutableSet()
            ?: mutableSetOf()
        aggregated.addAll(classes.keys)

        return aggregated
    }

    fun resolve(name: String) : DrValue? =
        values[name] ?: parent?.resolve(name)

    fun resolveClass(name: String) : DrClass? =
        classes[name] ?: parent?.resolveClass(name)

    fun copy() : DrEnv = DrEnv(parent, values, classes)

    fun isTopLevel() : Boolean = parent == null
}