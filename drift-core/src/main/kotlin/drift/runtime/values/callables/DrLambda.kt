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


/******************************************************************************
 * DRIFT LAMBDA RUNTIME TYPE
 *
 * Runtime class to represent Lambda statements.
 ******************************************************************************/



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
    override fun type(): DrType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        )
    )
}