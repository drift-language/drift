/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.values.specials

import drift.types.ParserType
import drift.values.Value
import drift.types.UnknownType


/******************************************************************************
 * DRIFT NOT ASSIGNED RUNTIME SPECIAL TYPE
 *
 * Runtime class for NotAssigned special type.
 ******************************************************************************/



/**
 * Runtime representation of the NotAssigned type, which represents
 * a variable which does not have a value.
 *
 * It is linked to [UnknownType].
 */
@Deprecated("Literal NULL value on a nullable ParserExpression? environment is preferred")
data object NotAssignedValue : Value {

    override fun asString(): String = UnknownType.asString()

    override fun type(): ParserType = UnknownType
}