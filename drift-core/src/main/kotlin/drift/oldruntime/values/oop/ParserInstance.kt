/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime.values.oop

import drift.oldruntime.DrEnv
import drift.oldruntime.ParserType
import drift.oldruntime.ParserValue
import drift.oldruntime.ObjectType
import drift.oldruntime.evaluators.eval
import drift.oldruntime.exceptions.DRMissingReturnStatementException
import drift.oldruntime.exceptions.DRUnknownClassMemberException
import drift.oldruntime.exceptions.DRUnsuccessfulCastException
import drift.oldruntime.exceptions.DRVariableNotDefinedException
import drift.oldruntime.values.callables.ParserMethod
import drift.oldruntime.values.callables.ParserReturn
import drift.oldruntime.values.primaries.ParserString


/******************************************************************************
 * DRIFT CLASS INSTANCE RUNTIME TYPE
 *
 * Runtime class to represent Class Instances.
 ******************************************************************************/



/**
 * Runtime representation of a class instance.
 */
data class ParserInstance(
    /** Class structure */
    val klass: ParserClass,

    /** Instance environment */
    val env: DrEnv) : ParserValue {



    /** @return A prepared string version of the type */
    override fun asString() : String {
        val default = "<[class#${klass.hashCode()}] ${klass.name} | instance ${this.hashCode()}>"

        val method = klass.methods["asString"]
            ?: return default

        val local = DrEnv()
        local.define("\$this", this)

        var result: ParserValue? = null

        for (statement in method.let.body.statements) {
            val evalResult = statement.eval(local)

            if (evalResult is ParserReturn) {
                result = evalResult.value
                break
            }

            result = evalResult
        }

        if (result == null)
            throw DRMissingReturnStatementException()

        if (result !is ParserString)
            throw DRUnsuccessfulCastException(
                valueType = result.type(),
                expectedType = ObjectType("String"))

        return result.asString()
    }

    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(klass.name)


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
    fun get(name: String) : ParserValue {
        if (has(name))
            return env.get(name)

        val method = klass.methods[name]

        if (method is ParserMethod) {
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
    fun set(name: String, value: ParserValue) {
        if (!has(name))
            throw DRVariableNotDefinedException(name = name)

        env.assign(name, value)
    }
}
