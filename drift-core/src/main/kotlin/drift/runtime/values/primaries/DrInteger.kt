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
 * DRIFT INTEGERS RUNTIME TYPE
 *
 * Runtime class for integer types.
 ******************************************************************************/



/**
 * This interface represents all integer value types.
 */
sealed interface DrInteger<T> : DrNumeric {
    val value: T
}