/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.callables

import drift.ast.statements.Func
import drift.oldruntime.DrEnv
import drift.oldruntime.ParserType
import drift.oldruntime.MultiTypes
import drift.oldruntime.ObjectType
import drift.oldruntime.ParserObject
import drift.oldruntime.SingleType


/******************************************************************************
 * DRIFT FUNCTION RUNTIME TYPE
 *
 * Runtime class to represent Function statements.
 ******************************************************************************/



/**
 * Runtime representation of a function.
 *
 * It does not represent [ParserLambda], [ParserNativeFunction]
 * and [ParserMethod].
 *
 * ```
 * fun test(arg) {
 *      // Body
 * }
 * ```
 *
 * @see ParserCallable
 */
data class ParserFunction(
    /** Function structure */
    val let: Func,

    /** Function closure, environment instance */
    val closure: DrEnv
) : ParserObject, ParserCallable {

    override val className = "Function"


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "<[function@${hashCode()}] ${let.name} ${type().asString()}>"

    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(
        className, mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        ))
}