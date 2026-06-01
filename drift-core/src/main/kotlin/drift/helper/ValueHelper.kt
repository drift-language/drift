/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.helper

import drift.oldruntime.ParserValue
import drift.oldruntime.exceptions.DRCannotUseUnassignedEntityException
import drift.oldruntime.exceptions.DRCannotUseVoidAsValueException
import drift.oldruntime.exceptions.DRNotSupportedTypeInRangeException
import drift.oldruntime.exceptions.DRRangeLimitsMustHaveSameTypeException
import drift.oldruntime.values.containers.range.ParserRange
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.primaries.ParserInt64
import drift.oldruntime.values.primaries.DrInteger
import drift.oldruntime.values.specials.ParserNotAssigned
import drift.oldruntime.values.specials.ParserVoid
import drift.oldruntime.values.variables.ParserVariable


/******************************************************************************
 * VALUE HELPER FUNCTIONS
 *
 * All Runtime functions that help to manipulate AST values objects
 * are defined in this file
 ******************************************************************************/



/**
 * Unwrap a AST value object
 *
 * @param value AST value object to unwrap
 * @return The contained value if exists;
 * else the provided value
 */
fun unwrap(value: ParserValue) : ParserValue {
    var current = value

    while (current is ParserVariable)
        current = current.value

    return current
}



/**
 * Validate a AST value object
 *
 * @param value AST value object to validate
 * @param ignoreNotAssigned If NotAssigned must
 * throw an exception (cannot use unassigned)
 * @param ignoreVoid If Void must throw an exception
 * (cannot use void)
 * @return Validated value
 * @throws DRCannotUseUnassignedEntityException
 * @throws DRCannotUseVoidAsValueException
 */
fun validateValue(value: ParserValue, ignoreNotAssigned: Boolean = false, ignoreVoid: Boolean = false) : ParserValue {
    return when (value) {
        is ParserNotAssigned ->
            if (ignoreNotAssigned) value
            else throw DRCannotUseUnassignedEntityException()
        is ParserVoid ->
            if (ignoreVoid) value
            else throw DRCannotUseVoidAsValueException()
        else -> value
    }
}



/**
 * Convert a Range (Inclusive or Exclusive) to a List
 *
 * @param range Range to convert
 * @return Converted List of integers
 * @throws DRRangeLimitsMustHaveSameTypeException
 * @throws DRNotSupportedTypeInRangeException
 */
fun rangeToList(range: ParserRange, exclusive: Boolean = false): List<DrInteger<*>> {
    return when {
        range.from is ParserInt && range.to is ParserInt ->
            if (!exclusive) (range.from.value as Int..range.to.value as Int).map { ParserInt(it) }
            else (range.from.value as Int..<range.to.value as Int).map { ParserInt(it) }

        range.from is ParserInt64 && range.to is ParserInt64 ->
            if (!exclusive) (range.from.value as Long..range.to.value as Long).map { ParserInt64(it) }
            else (range.from.value as Long..<range.to.value as Long).map { ParserInt64(it) }

        range.from::class != range.to::class ->
            throw DRRangeLimitsMustHaveSameTypeException()

        else -> throw DRNotSupportedTypeInRangeException(
            type = (range.from as ParserValue).type())
    }
}