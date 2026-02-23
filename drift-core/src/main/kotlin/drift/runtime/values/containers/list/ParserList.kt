/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.containers.list

import drift.runtime.AnyType
import drift.runtime.ParserType
import drift.runtime.ParserValue
import drift.runtime.ObjectType
import drift.runtime.SingleType
import drift.runtime.UnionType


/******************************************************************************
 * DRIFT LIST RUNTIME TYPE
 *
 * Runtime class to represent List type.
 ******************************************************************************/



/**
 * Runtime List structure.
 *
 * A List is an auto-incremented index-based container.
 *
 * ### Syntax
 * Type: ```[Type]```
 * ```drift
 * let names: [String] = [ ... ]
 * ```
 */
data class ParserList(
    /** List values */
    val items: List<ParserValue>) : ParserValue {


    /** @return A prepared string version of the type */
    override fun asString(): String =
        "[ ${items.joinToString(", ") { it.asString() }} ]"


    /** @return The object representation of the type */
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