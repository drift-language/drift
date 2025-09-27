/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.specials

import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.NullType


/******************************************************************************
 * DRIFT NULL RUNTIME SPECIAL TYPE
 *
 * Runtime class for Null special type.
 ******************************************************************************/



/**
 * AST representation of the NULL type, which represents
 * the absence of a value in Drift.
 *
 * It is used for variables or expressions that do not
 * reference any object or value.
 *
 * @see drift.runtime.NullType
 */
data object DrNull : DrSpecial {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}