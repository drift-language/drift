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
 * DRIFT BOOLEAN RUNTIME TYPE
 *
 * Runtime class for Boolean type.
 ******************************************************************************/



/**
 * Runtime representation of a boolean.
 *
 * @see DrPrimary
 */
data class ParserBool(
    /** Boolean value */
    override val value: Boolean)
    : ParserObject, DrPrimary<Boolean> {

    override val className = ParserPrimitiveClass.Bool.className


    /** @return A prepared string version of the type */
    override fun asString() = value.toString()
}