/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.oldruntime.values.specials

import drift.oldruntime.ParserType
import drift.oldruntime.NullType


/******************************************************************************
 * DRIFT NULL RUNTIME SPECIAL TYPE
 *
 * Runtime class for Null special type.
 ******************************************************************************/



/**
 * Runtime representation of the NULL type, which represents
 * the absence of a value in Drift.
 *
 * It is used for variables or expressions that do not
 * reference any object or value.
 *
 * @see drift.oldruntime.NullType
 */
data object ParserNull : ParserSpecial {
    override fun asString() = "null"

    override fun type(): ParserType = NullType
}