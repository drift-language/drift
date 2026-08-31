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
 * DRIFT BOOLEAN RUNTIME TYPE
 *
 * Runtime class for Boolean type.
 ******************************************************************************/



/**
 * Runtime representation of a boolean.
 *
 * @see PrimaryValue
 */
data class BoolValue(
    /** Boolean value */
    override val value: Boolean)
    : ObjectValue, PrimaryValue<Boolean> {

    override val qualifiedName = ParserPrimitiveClass.Bool.qualifiedName


    /** @return A prepared string version of the type */
    override fun asString() = value.toString()
}