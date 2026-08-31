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
 * DRIFT STRING RUNTIME TYPE
 *
 * Runtime class for String type.
 ******************************************************************************/



/**
 * Runtime representation of a string.
 *
 * @see PrimaryValue
 */
data class StringValue(
    /** String value (unquoted) */
    override val value: String) : ObjectValue, PrimaryValue<String> {

    override val qualifiedName = ParserPrimitiveClass.String.qualifiedName


    override fun asString() = value
}