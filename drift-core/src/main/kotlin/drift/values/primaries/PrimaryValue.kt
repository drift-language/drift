/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.values.primaries


/******************************************************************************
 * DRIFT PRIMARY TYPE INTERFACE
 *
 * Interface for all Drift primary types.
 ******************************************************************************/



/**
 * This interface represents all primary value types.
 */
sealed interface PrimaryValue<T> {

    val value: T
}