/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.callables

import drift.oldruntime.AnyType
import drift.oldruntime.ParserType
import drift.oldruntime.ParserValue
import drift.oldruntime.MultiTypes
import drift.oldruntime.ObjectType
import drift.oldruntime.SingleType


/******************************************************************************
 * DRIFT NATIVE FUNCTION RUNTIME TYPE
 *
 * Runtime class to represent Native Function structures.
 ******************************************************************************/



/**
 * Runtime representation of a native function.
 *
 * It does not represent [ParserFunction], [ParserLambda]
 * and [ParserMethod].
 *
 * @see ParserCallable
 */
data class ParserNativeFunction(
    /** Function optional name */
    val name: String? = null,

    /** Kotlin function source code callback */
    val impl: (receiver: ParserValue?, args: List<Pair<String?, ParserValue>>) -> ParserValue,

    /** Types of [impl] arguments, in same order */
    val paramTypes: List<ParserType>,

    /** Function return type */
    val returnType: ParserType = AnyType
) : ParserValue, ParserCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[native#${hashCode()}] ${type().asString()}>"


    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(paramTypes),
            "returnType" to SingleType(returnType)
        )
    )
}