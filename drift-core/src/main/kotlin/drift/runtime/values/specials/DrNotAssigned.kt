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
import drift.runtime.UnknownType


/******************************************************************************
 * DRIFT NOT ASSIGNED RUNTIME SPECIAL TYPE
 *
 * Runtime class for NotAssigned special type.
 ******************************************************************************/



/**
 * AST representation of the NotAssigned type, which represents
 * a variable which does not have a value.
 *
 * It is linked to [UnknownType].
 */
data object DrNotAssigned : DrValue {
    override fun asString(): String = UnknownType.asString()
    override fun type(): DrType = UnknownType
}