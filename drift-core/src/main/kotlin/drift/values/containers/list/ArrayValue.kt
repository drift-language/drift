/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values.containers.list

import drift.types.AnyType
import drift.types.ParserType
import drift.values.Value
import drift.types.ObjectType
import drift.types.SingleType
import drift.types.UnionType
import drift.values.ObjectValue
import drift.values.ParserPrimitiveClass
import language.QualifiedName


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
data class ArrayValue(
    /** Array values */
    val items: List<Value>) : ObjectValue {


    override val qualifiedName: QualifiedName = ParserPrimitiveClass.Array.qualifiedName


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
            qualifiedName,
            args = mapOf("type" to SingleType(type)))
    }
}