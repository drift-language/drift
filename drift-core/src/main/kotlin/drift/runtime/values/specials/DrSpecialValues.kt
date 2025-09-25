/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.values.specials

import drift.runtime.*

/******************************************************************************
 * DRIFT SPECIAL VALUE TYPES STRUCTURES
 *
 * All special value types are defined in this file.
 * A special value type apply its behavior on runtime evaluation.
 ******************************************************************************/



/**
 * AST representation of the NULL type, which represents
 * the absence of a value in Drift.
 *
 * It is used for variables or expressions that do not
 * reference any object or value.
 *
 * @see NullType
 */
data object DrNull : DrValue {
    override fun asString() = "null"

    override fun type(): DrType = NullType
}



/**
 * AST representation of the VOID type, which represents
 * the absence of return for a function.
 *
 * @see VoidType
 */
data object DrVoid : DrValue {
    override fun asString() = "void"

    override fun type() = VoidType
}



/**
 * AST representation of the NotAssigned type, which represents
 * a variable which does not have a value.
 *
 * It is linked to [UnknownType].
 */
data object DrNotAssigned : DrValue {
    override fun asString(): String = UnknownType.asString()
    override fun type(): DrType = UnknownType
}