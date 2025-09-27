/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.primaries


/******************************************************************************
 * DRIFT PRIMARY RUNTIME TYPE
 *
 * Interface for all Drift primary types.
 ******************************************************************************/



/**
 * This interface represents all primary value types.
 */
sealed interface DrPrimary<T> {
    val value: T
}