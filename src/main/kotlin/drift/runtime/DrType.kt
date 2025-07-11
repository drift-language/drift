/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime


/******************************************************************************
 * DRIFT TYPES
 *
 * Type definitions and hierarchy used across the language.
 ******************************************************************************/



/**
 * This interface represents the whole special and complex
 * types, like null, void, any, last, unknown, classes, and
 * united, optional types.
 */
sealed interface DrType {
    /** @return A prepared string version of the type */
    fun asString() : String
}



/**********************************
 * SPECIAL TYPES
 **********************************/


/**
 * NULL type represents the absence of a value in Drift.
 *
 * It is used for variables or expressions that do not
 * reference any object or value.
 *
 * @see DrType
 */
data object NullType : DrType {
    override fun asString(): String = "Null"
}



/**
 * VOID type represents the absence of return for a function.
 *
 * @see DrType
 */
data object VoidType : DrType {
    override fun asString(): String = "Void"
}



/**
 * ANY type represents the absence of type for a variable,
 * function return, parameter, etc.
 *
 * ANY is applied to any variable, function without explicit
 * type.
 *
 * @see DrType
 */
data object AnyType : DrType {
    override fun asString(): String = "Any"
}



/**
 * LAST special type allows the function to use the last
 * expression as return value.
 *
 * ```
 * // This function returns 1
 * fun test : Last {
 *      1
 * }
 * ```
 *
 * @see DrType
 */
data object LastType : DrType {
    override fun asString(): String = "Last"
}



/**
 * UNKNOWN special type represents a variable which does not
 * have a value.
 *
 * It is linked to [drift.runtime.values.specials.DrNotAssigned].
 *
 * @see DrType
 */
data object UnknownType : DrType {
    override fun asString(): String = "Unknown"
}



/**********************************
 * TYPE ENRICHING CONTAINERS
 **********************************/


/**
 * This type container add the optional behavior
 * to inner type. It allows to use NULL as value.
 *
 * By default, an entity is non-nullable.
 *
 * A type is optional if `?` character follows it.
 *
 * ```
 * var optional: String? = null
 * optional: String? = "Hello!"
 * ```
 *
 * @param inner Inner type to make optional
 * @see DrType
 */
data class OptionalType(val inner: DrType) : DrType {
    override fun asString() = "${inner.asString()}?"
}


/**
 * This type container permits to unite provided
 * types. It allows to type an entity with many types.
 *
 * Many types can be united using the `|` character between
 * them.
 *
 * ```
 * var united: String|Int = "Hello"
 * united = 1
 * ```
 *
 * @param options United types (inner)
 * @see DrType
 */
data class UnionType(val options: List<DrType>) : DrType {
    override fun asString() = options.joinToString(" | ") { it.asString() }
}



/**********************************
 * OOP MAIN TYPES
 **********************************/


/**
 * This type represents a Drift object, inherited
 * from a class.
 *
 * Drift represents all types as objects from injected
 * native classes.
 *
 * @param className Object's class name
 * @param args Object arguments
 */
data class ObjectType(val className: String, val args: Map<String, TypeArgument> = emptyMap()) : DrType {
    override fun asString() = className
}



/**
 * Verify if the provided value type could be used
 * with expected one.
 *
 * This function should be used before any entity assign.
 *
 * @param valueType Type of the value to assign
 * @param expected Expected type from entity
 * @return If both types can cooperate on assign
 */
fun isAssignable(valueType: DrType, expected: DrType): Boolean {
    if (valueType == UnknownType || expected == AnyType)
        return true

    if (expected is ObjectType && valueType is ObjectType)
        return expected.className == valueType.className

    return when (expected) {
        is OptionalType -> valueType == NullType || isAssignable(valueType, expected.inner)
        is UnionType -> expected.options.any { isAssignable(valueType, it) }
        else -> false
    }
}