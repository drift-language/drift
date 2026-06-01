/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.specials

import drift.oldruntime.ParserValue
import drift.oldruntime.VoidType


/******************************************************************************
 * DRIFT VOID RUNTIME SPECIAL TYPE
 *
 * Runtime class for Void special type.
 ******************************************************************************/



/**
 * Runtime representation of the VOID type, which represents
 * the absence of return for a function.
 *
 * @see drift.oldruntime.VoidType
 */
data object ParserVoid : ParserValue {
    override fun asString() = "void"

    override fun type() = VoidType
}