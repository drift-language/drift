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
sealed interface ParserType {
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
 * @see ParserType
 */
data object NullType : ParserType {
    override fun asString(): String = "Null"
}



/**
 * VOID type represents the absence of return for a function.
 *
 * @see ParserType
 */
data object VoidType : ParserType {
    override fun asString(): String = "Void"
}



/**
 * ANY type represents the absence of type for a variable,
 * function return, parameter, etc.
 *
 * ANY is applied to any variable, function without explicit
 * type.
 *
 * @see ParserType
 */
data object AnyType : ParserType {
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
 * @see ParserType
 */
data object LastType : ParserType {
    override fun asString(): String = "Last"
}



/**
 * UNKNOWN special type represents a variable which does not
 * have a value.
 *
 * It is linked to [drift.runtime.values.specials.ParserNotAssigned].
 *
 * @see ParserType
 */
data object UnknownType : ParserType {
    override fun asString(): String = "Unknown"
}



/**********************************
 * TYPE ENRICHING CONTAINERS
 **********************************/


/**
 * This type container adds the optional behavior
 * to the inner type. It allows using NULL as a value.
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
 * @see ParserType
 */
data class OptionalType(val inner: ParserType) : ParserType {
    override fun asString() = "${inner.asString()}?"
}


/**
 * This type container permits uniting provided
 * types. It allows typing an entity with many types.
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
 * @see ParserType
 */
data class UnionType(val options: List<ParserType>) : ParserType {
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
data class ObjectType(val className: String, val args: Map<String, TypeArgument> = emptyMap()) : ParserType {

    constructor(primitive: ParserPrimitiveClass, args: Map<String, TypeArgument> = emptyMap())
    : this(primitive.className, args)

    fun isPrimitiveNumeric() =
        isPrimitiveInt() || isPrimitiveInt64() || isPrimitiveUInt()

    fun isPrimitiveInt() = className == ParserPrimitiveClass.Int.className
    fun isPrimitiveInt64() = className == ParserPrimitiveClass.Int64.className
    fun isPrimitiveUInt() = className == ParserPrimitiveClass.UInt.className
    fun isPrimitiveString() = className == ParserPrimitiveClass.String.className
    fun isPrimitiveBool() = className == ParserPrimitiveClass.Bool.className


    override fun asString() = className
}


/**
 * This type represents a Drift function or lambda type.
 *
 * It carries the parameter types and return type explicitly,
 * making the structure self-describing for backends and type checks.
 *
 * @param paramTypes Types of the function parameters
 * @param returnType Type of the return value
 */
data class FunctionType(
    val paramTypes: List<ParserType> = emptyList(),
    val returnType: ParserType = AnyType) : ParserType {
    
    override fun asString() : String {
        val params = paramTypes.joinToString(", ") { it.asString() }
        
        return "($params) -> ${returnType.asString()}"
    }
}


/**
 * This type represents a Drift class.
 *
 * Drift represents all classes using this parser type.
 *
 * @param className
 * @param generics
 */
data class ClassType(
    val className: String,
    val generics: Map<String, ParserType> = emptyMap()) : ParserType {

    override fun asString(): String {
        return if (generics.isEmpty()) "Class<$className>"
               else {
                   val genericsAsString = generics.values
                       .joinToString(", ") { it.asString() }

                   "Class<$className<$genericsAsString>>"
               }
    }
}


/**
 * This type represents a Drift array.
 *
 * Drift represents all arrays using this parser type.
 */
data class ArrayType(
    val type: ParserType) : ParserType {

    override fun asString(): String =
        "($type[])"
}

/**
 * Verify if the provided value type could be used
 * with the expected one.
 *
 * This function should be used before any entity is assigned.
 *
 * @param valueType Type of the value to assign
 * @param expected Expected type from entity
 * @return If both types can cooperate on assign
 */
fun isAssignable(valueType: ParserType, expected: ParserType): Boolean {
    if (valueType == UnknownType
        || expected == AnyType
        || expected == VoidType && valueType == VoidType) {

        return true
    }

    if (expected is ObjectType && valueType is ObjectType)
        return expected.className == valueType.className

    if (expected is FunctionType && valueType is FunctionType)
        return expected.paramTypes == valueType.paramTypes && 
               expected.returnType == valueType.returnType

    return when (expected) {
        is OptionalType -> valueType == NullType || isAssignable(valueType, expected.inner)
        is UnionType ->
            expected.options.contains(NullType) && valueType == NullType
            || expected.options.any { isAssignable(valueType, it) }
        else -> false
    }
}