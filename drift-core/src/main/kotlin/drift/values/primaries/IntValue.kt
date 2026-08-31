/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values.primaries

import drift.types.ObjectType
import drift.values.ObjectValue
import drift.values.ParserPrimitiveClass
import drift.types.ParserType


/******************************************************************************
 * DRIFT INTEGER RUNTIME TYPE
 *
 * Runtime class for Int type.
 ******************************************************************************/



/**
 * Runtime representation of a 32-bits integer.
 *
 * @see PrimaryValue
 */
data class IntValue(
    /** Integer value */
    override val value: Int) : PrimaryValue<Int>, ObjectValue {

    override val qualifiedName = ParserPrimitiveClass.Int.qualifiedName


    override fun asString() = value.toString()
}