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
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.MultiTypes
import drift.runtime.ObjectType
import drift.runtime.SingleType


/******************************************************************************
 * DRIFT FUNCTION RUNTIME TYPE
 *
 * Runtime class to represent Function statements.
 ******************************************************************************/



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
    val closure: DrEnv
) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[function@${hashCode()}] ${let.name} ${type().asString()}>"

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        )
    )
}