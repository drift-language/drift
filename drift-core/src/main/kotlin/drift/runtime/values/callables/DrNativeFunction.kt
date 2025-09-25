/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.callables

import drift.runtime.AnyType
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.MultiTypes
import drift.runtime.ObjectType
import drift.runtime.SingleType


/******************************************************************************
 * DRIFT NATIVE FUNCTION RUNTIME TYPE
 *
 * Runtime class to represent Native Function structures.
 ******************************************************************************/



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
    val returnType: DrType = AnyType
) : DrValue, DrCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[native#${hashCode()}] ${type().asString()}>"
    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(paramTypes),
            "returnType" to SingleType(returnType)
        )
    )
}