/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.containers.range

import drift.runtime.DrValue
import drift.runtime.values.primaries.DrInteger


/******************************************************************************
 * DRIFT RANGE RUNTIME TYPES
 *
 * Interface for all Drift Range types.
 ******************************************************************************/



/**
 * Interface representing all Drift Range types.
 *
 * At this moment, it exists two variants:
 * - [DrInclusiveRange] `a..b`
 * - [DrExclusiveRange] `a..<b`
 *
 * @see DrInclusiveRange
 * @see DrExclusiveRange
 */
sealed interface DrRange : DrValue {
    /** From value, start of the range */
    val from: DrInteger<*>

    /** To value, end of the range, included */
    val to: DrInteger<*>
}