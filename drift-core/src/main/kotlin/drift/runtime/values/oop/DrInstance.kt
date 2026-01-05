/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.oop

import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.evaluators.eval
import drift.runtime.exceptions.DRMissingReturnStatementException
import drift.runtime.exceptions.DRUnknownClassMemberException
import drift.runtime.exceptions.DRUnsuccessfulCastException
import drift.runtime.exceptions.DRVariableNotDefinedException
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

    /** Instance environment */
    val env: DrEnv) : DrValue {



    /** @return A prepared string version of the type */
    override fun asString() : String {
        val default = "<[class#${klass.hashCode()}] ${klass.name} | instance ${this.hashCode()}>"

        val method = klass.methods["asString"]
            ?: return default

        val local = DrEnv()
        local.define("\$this", this)

        var result: DrValue? = null

        for (statement in method.let.body) {
            val evalResult = statement.eval(local)

            if (evalResult is DrReturn) {
                result = evalResult.value
                break
            }

            result = evalResult
        }

        if (result == null)
            throw DRMissingReturnStatementException()

        if (result !is DrString)
            throw DRUnsuccessfulCastException(
                valueType = result.type(),
                expectedType = ObjectType("String"))

        return result.asString()
    }

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(klass.name)


    /**
     * @return If provided name is a defined key in the value map
     */
    fun has(name: String) : Boolean =
        env.exists(name)


    /**
     * Attempt to get the attribute's value using its name.
     *
     * @param name Attribute name
     * @return Attribute value
     */
    fun get(name: String) : DrValue {
        if (has(name))
            return env.get(name)

        val method = klass.methods[name]

        if (method is DrMethod) {
            return method.copy(instance = this)
        }

        throw DRUnknownClassMemberException(
            memberName = name,
            className = klass.name)
    }



    /**
     * Attempt to set the attribute's value.
     *
     * @param name Attribute name
     * @param value New attribute value to apply
     */
    fun set(name: String, value: DrValue) {
        if (!has(name))
            throw DRVariableNotDefinedException(name = name)

        env.assign(name, value)
    }
}
