/*
 * Drift Programming Language
 * Drift JVM Backend
 *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)
 *
 * This source code is licensed under the MIT License.
 * See the LICENSE file in the root directory for details.
 */

package drift.oldruntime.values.primaries

import drift.oldruntime.NullType
import drift.oldruntime.ParserType

/******************************************************************************
 * DRIFT NULL LITERAL VALUE
 *
 * Runtime class for null literal value.
 ******************************************************************************/


/**
 * Runtime representation of the NULL value, which represents
 * the absence of a value in Drift.
 *
 * It is used for variables or expressions that do not
 * reference any object or value.
 *
 * @see drift.oldruntime.NullType
 */
data object ParserNull : DrPrimary<Nothing?> {

    override val value: Nothing? = null


    fun asString() = "null"

    @Deprecated("From old interpreter. Will be deleted.")
    fun type(): ParserType = NullType
}