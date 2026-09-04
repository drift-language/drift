/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values

import drift.types.Type


/******************************************************************************
 * DRIFT VALUES
 *
 * Core runtime value interface.
 ******************************************************************************/



/**
 * This interface represents the whole types existing
 * natively in Drift, like primary ones, classes,
 * callables, etc.
 */
interface Value {

    fun asString() : String

    @Deprecated("To delete with old interpreter")
    fun type() : Type
}