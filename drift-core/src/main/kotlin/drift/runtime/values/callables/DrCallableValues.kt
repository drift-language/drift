/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.callables

import drift.runtime.*


/******************************************************************************
 * DRIFT CALLABLE VALUE TYPES
 *
 * All callable value types are defined in this file.
 ******************************************************************************/



/**
 * AST representation of a callable return statement.
 */
data class DrReturn(val value: DrValue) : DrValue {
    /** @return A prepared string version of the type */
    override fun asString(): String = value.asString()

    /** @return The object representation of the type */
    override fun type(): DrType = value.type()
}