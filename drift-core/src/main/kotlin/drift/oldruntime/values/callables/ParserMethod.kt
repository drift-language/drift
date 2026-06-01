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
import drift.oldruntime.ParserValue
import drift.oldruntime.MultiTypes
import drift.oldruntime.ObjectType
import drift.oldruntime.SingleType
import drift.oldruntime.values.oop.ParserInstance


/******************************************************************************
 * DRIFT CLASS METHOD RUNTIME TYPE
 *
 * Runtime class to represent Class Method statements.
 ******************************************************************************/



/**
 * Runtime representation of a class method.
 *
 * It does not represent [ParserFunction], [ParserNativeFunction]
 * and [ParserLambda].
 *
 * @see ParserCallable
 */
data class ParserMethod(
    /** Method structure */
    val let: Func,

    /** Method closure, environment instance */
    val closure: DrEnv,

    /** Method's class instance if existing; else NULL */
    val instance: ParserValue? = null,

    /**
     *   **Only for native methods**
     *
     * Drift native method structure
     */
    val nativeImpl: ParserNativeFunction? = null) : ParserValue, ParserCallable {



    /** @return A prepared string version of the type */
    override fun asString(): String {
        val i: ParserInstance? = instance as? ParserInstance
        var sourceString = "#"

        if (i is ParserInstance) {
            sourceString = "${i.klass.name}#${i.hashCode()}"
        }

        return "<[function#${hashCode()}] $sourceString.${let.name} ${type().asString()}>"
    }

    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(
        "Function", mapOf(
            "paramTypes" to MultiTypes(let.parameters.map { it.type }),
            "returnType" to SingleType(let.returnType)
        )
    )
}