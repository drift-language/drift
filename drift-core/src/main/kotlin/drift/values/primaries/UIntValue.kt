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
 * DRIFT UNSIGNED INTEGER RUNTIME TYPE
 *
 * Runtime class for Unsigned Integer type.
 ******************************************************************************/



/**
 * Runtime representation of a 32-bits unsigned integer.
 *
 * @see PrimaryValue
 */
data class UIntValue(
    /** Integer value */
    override val value: UInt) : PrimaryValue<UInt>, ObjectValue {

    override val qualifiedName = ParserPrimitiveClass.UInt.qualifiedName


    override fun asString() = value.toString()
}
