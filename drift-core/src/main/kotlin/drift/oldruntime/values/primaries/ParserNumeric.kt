/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.primaries


/******************************************************************************
 * DRIFT UNRESOLVED NUMERIC LITERAL
 *
 * Parse-time representation of an integer literal whose concrete type
 * (Int, Int64, UInt, …) has not yet been decided. Stored as Long to
 * accommodate every currently supported integer range without loss.
 * Type inference assigns the concrete type based on context.
 ******************************************************************************/



/**
 * An integer literal whose concrete type is resolved by type inference.
 *
 * @property value Raw integer value, stored as Long
 */
data class ParserNumeric(override val value: Long) : DrPrimary<Long>
