/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values.primaries

import drift.values.ObjectValue
import drift.values.ParserPrimitiveClass


/******************************************************************************
 * DRIFT 64-BITS INTEGER RUNTIME TYPE
 *
 * Runtime class for 64-bits Integer type.
 ******************************************************************************/



/**
 * Runtime representation of a 64-bits integer.
 *
 * @see PrimaryValue
 */
data class Int64Value(
    /** Integer value */
    override val value: Long) : PrimaryValue<Long>, ObjectValue {

    override val qualifiedName = ParserPrimitiveClass.Int64.qualifiedName


    override fun asString() = value.toString()
}