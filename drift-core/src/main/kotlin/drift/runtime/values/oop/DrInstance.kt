/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.oop

import drift.exceptions.DriftRuntimeException
import drift.exceptions.DriftTypeException
import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.evaluators.eval
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.callables.DrReturn
import drift.runtime.values.primaries.DrString


/******************************************************************************
 * DRIFT CLASS INSTANCE RUNTIME TYPE
 *
 * Runtime class to represent Class Instances.
 ******************************************************************************/



/**
 * AST representation of a class instance.
 */
data class DrInstance(
    /** Class structure */
    val klass: DrClass,

    /** Class attributes map */
    val values: MutableMap<String, DrValue>) : DrValue {



    /** @return A prepared string version of the type */
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

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(klass.name)



    /**
     * Attempt to get the attribute's value using its name.
     *
     * @param name Attribute name
     * @return Attribute value
     * @throws DriftRuntimeException If the provided name
     * is no longer recorded into instance's attributes map
     */
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



    /**
     * Attempt to set the attribute's value.
     *
     * @param name Attribute name
     * @param value New attribute value to apply
     * @throws DriftRuntimeException If the required variable
     * is no longer declared
     */
    fun set(name: String, value: DrValue) {
        if (!values.containsKey(name))
            throw DriftRuntimeException("Cannot assign to undeclared property '${klass.name}.$name'")

        values[name] = value
    }
}
