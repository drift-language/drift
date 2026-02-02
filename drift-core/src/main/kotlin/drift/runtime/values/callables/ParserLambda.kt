/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.callables

import drift.ast.statements.Function
import drift.runtime.DrEnv
import drift.runtime.ParserType
import drift.runtime.ParserValue
import drift.runtime.MultiTypes
import drift.runtime.ObjectType
import drift.runtime.SingleType


/******************************************************************************
 * DRIFT LAMBDA RUNTIME TYPE
 *
 * Runtime class to represent Lambda statements.
 ******************************************************************************/



/**
 * Runtime representation of a lambda function.
 *
 * It does not represent [ParserFunction], [ParserNativeFunction]
 * and [ParserMethod].
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
 * @see ParserCallable
 */
data class ParserLambda(
    /** Lambda function structure */
    val let: Function,

    /** Lambda function closure, environment instance */
    val closure: DrEnv,

    /** Captured parent environment entities */
    val captures: Map<String, ParserValue>) : ParserValue, ParserCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[lambda#${hashCode()}] ${type().asString()}>"

    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        )
    )
}