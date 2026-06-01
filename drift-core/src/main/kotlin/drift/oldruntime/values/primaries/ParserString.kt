/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.primaries

import drift.oldruntime.ParserObject
import drift.oldruntime.ParserPrimitiveClass


/******************************************************************************
 * DRIFT STRING RUNTIME TYPE
 *
 * Runtime class for String type.
 ******************************************************************************/



/**
 * Runtime representation of a string.
 *
 * @see DrPrimary
 */
data class ParserString(
    /** String value (unquoted) */
    override val value: String) : ParserObject, DrPrimary<String> {

    override val className = ParserPrimitiveClass.String.className


    override fun asString() = value
}