/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.callables

import drift.ast.Function
import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.MultiTypes
import drift.runtime.ObjectType
import drift.runtime.SingleType
import drift.runtime.values.oop.DrInstance


/******************************************************************************
 * DRIFT CLASS METHOD RUNTIME TYPE
 *
 * Runtime class to represent Class Method statements.
 ******************************************************************************/



/**
 * AST representation of a class method.
 *
 * It does not represent [DrFunction], [DrNativeFunction]
 * and [DrLambda].
 *
 * @see DrCallable
 */
data class DrMethod(
    /** Method structure */
    val let: Function,

    /** Method closure, environment instance */
    val closure: DrEnv,

    /** Method's class instance if existing; else NULL */
    val instance: DrValue? = null,

    /**
     *   **Only for native methods**
     *
     * Drift native method structure
     */
    val nativeImpl: DrNativeFunction? = null) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String {
        val i: DrInstance? = instance as? DrInstance
        var sourceString = "#"

        if (i is DrInstance) {
            sourceString = "${i.klass.name}#${i.hashCode()}"
        }

        return "<[function#${hashCode()}] $sourceString.${let.name} ${type().asString()}>"
    }

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        )
    )
}