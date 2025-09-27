/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.specials

import drift.runtime.DrValue
import drift.runtime.VoidType


/******************************************************************************
 * DRIFT VOID RUNTIME SPECIAL TYPE
 *
 * Runtime class for Void special type.
 ******************************************************************************/



/**
 * AST representation of the VOID type, which represents
 * the absence of return for a function.
 *
 * @see drift.runtime.VoidType
 */
data object DrVoid : DrValue {
    override fun asString() = "void"

    override fun type() = VoidType
}