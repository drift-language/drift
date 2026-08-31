/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values.specials

import drift.values.Value
import drift.types.VoidType


/******************************************************************************
 * DRIFT VOID RUNTIME SPECIAL TYPE
 *
 * Runtime class for Void special type.
 ******************************************************************************/



/**
 * Runtime representation of the VOID type, which represents
 * the absence of return for a function.
 *
 * @see drift.types.VoidType
 */
data object VoidValue : Value {
    override fun asString() = "void"

    override fun type() = VoidType
}