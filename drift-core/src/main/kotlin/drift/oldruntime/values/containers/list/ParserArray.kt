/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.containers.list

import drift.oldruntime.AnyType
import drift.oldruntime.ParserType
import drift.oldruntime.ParserValue
import drift.oldruntime.ObjectType
import drift.oldruntime.SingleType
import drift.oldruntime.UnionType


/******************************************************************************
 * DRIFT LIST RUNTIME TYPE
 *
 * Runtime class to represent List type.
 ******************************************************************************/



/**
 * Runtime Array structure.
 *
 * An Array is a fixedly sized container storing
 * one type of value.
 *
 * ### Syntax
 * Type: ```Type[]```
 * ```drift
 * let names: String[] = [ ... ]
 * ```
 */
data class ParserArray(
    /** Array values */
    val items: List<ParserValue>) : ParserValue {


    override fun asString(): String =
        "[ ${items.joinToString(", ") { it.asString() }} ]"


    @Deprecated("To delete with old interpreter")
    override fun type(): ParserType {
        val types = items.map { it.type() }.toSet()

        val type: ParserType = when {
            types.isEmpty() -> AnyType
            types.size == 1 -> types.first()

            else -> UnionType(types.toList())
        }

        return ObjectType(
            "List", mapOf(
                "type" to SingleType(type)))
    }
}