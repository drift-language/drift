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
 * INTEGER UTIL FUNCTIONS
 *
 * Utility functions for integers.
 ******************************************************************************/



/**
 * Concatenate two integers
 *
 * @return Result parsed in integer
 */
infix fun Int.concat(other: Int) = "$this$other".toInt()