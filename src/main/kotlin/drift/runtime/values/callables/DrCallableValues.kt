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
import drift.ast.eval
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import drift.runtime.values.oop.DrInstance


/******************************************************************************
 * DRIFT CALLABLE VALUE TYPES
 *
 * All callable value types are defined in this file.
 ******************************************************************************/



/**
 * This interface represents all callable value types.
 */
interface DrCallable



/**
 * AST representation of a function.
 *
 * It does not represent [DrLambda], [DrNativeFunction]
 * and [DrMethod].
 *
 * ```
 * fun test(arg) {
 *      // Body
 * }
 * ```
 *
 * @see DrCallable
 */
data class DrFunction(
    /** Function structure */
    val let: Function,

    /** Function closure, environment instance */
    val closure: DrEnv) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[function@${hashCode()}] ${let.name} ${type().asString()}>"

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType("Function", mapOf(
        "paramTypes" to MultiTypes(let.parameters.map { it.type }),
        "returnType" to SingleType(let.returnType)
    ))
}



/**
 * AST representation of a lambda function.
 *
 * It does not represent [DrFunction], [DrNativeFunction]
 * and [DrMethod].
 *
 * A lambda is an anonymous function that can
 * be defined into a variable, or returned by
 * another function, for example.
 *
 * Unlike a function or method, a lambda captures
 * its parent environment entities, any change outside
 * its scope will not affect the captured entities.
 *
 * ```
 * let lambda = (arg) -> {
 *      // Body
 * }
 * ```
 *
 * @see DrCallable
 */
data class DrLambda(
    /** Lambda function structure */
    val let: Function,

    /** Lambda function closure, environment instance */
    val closure: DrEnv,

    /** Captured parent environment entities */
    val captures: Map<String, DrValue>) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[lambda#${hashCode()}] ${type().asString()}>"

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType("Function", mapOf(
        "paramTypes" to MultiTypes(let.parameters.map { it.type }),
        "returnType" to SingleType(let.returnType)
    ))
}



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
    override fun type(): DrType = ObjectType("Function", mapOf(
        "paramTypes" to MultiTypes(let.parameters.map { it.type }),
        "returnType" to SingleType(let.returnType)
    ))
}



/**
 * AST representation of a native function.
 *
 * It does not represent [DrFunction], [DrLambda]
 * and [DrMethod].
 *
 * @see DrCallable
 */
data class DrNativeFunction(
    /** Function optional name */
    val name: String? = null,

    /** Kotlin function source code callback */
    val impl: (receiver: DrValue?, args: List<Pair<String?, DrValue>>) -> DrValue,

    /** Types of [impl] arguments, in same order */
    val paramTypes: List<DrType>,

    /** Function return type */
    val returnType: DrType = AnyType) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[native#${hashCode()}] ${type().asString()}>"
    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType("Function", mapOf(
        "paramTypes" to MultiTypes(paramTypes),
        "returnType" to SingleType(returnType)
    ))
}



/**
 * AST representation of a callable return statement.
 */
data class DrReturn(val value: DrValue) : DrValue {
    /** @return A prepared string version of the type */
    override fun asString(): String = value.asString()

    /** @return The object representation of the type */
    override fun type(): DrType = value.type()
}