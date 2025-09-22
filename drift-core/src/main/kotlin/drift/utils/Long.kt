/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.utils

/******************************************************************************
 * LONG UTIL FUNCTIONS
 *
 * Utility functions for longs.
 ******************************************************************************/



/**
 * Concatenate two longs
 *
 * @return Result parsed in long
 */
infix fun Long.concat(other: Long) = "$this$other".toLong()