/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.types

import drift.values.ParserPrimitiveClass
import language.QualifiedName


/******************************************************************************
 * DRIFT RESOLVED TYPES
 *
 * Type definitions and hierarchy used across the language once name
 * resolution has happened: every structure reference carries a real
 * [QualifiedName] (module, namespace, simple name).
 ******************************************************************************/



/**
 * This interface represents every fully resolved type used across the
 * language, from special types (null, void, any, unknown) to classes and
 * their compositions (optional, united types).
 *
 * **Attention!** [Type] must not be confused with [UnresolvedType]. A value
 * of [Type] always carries enough identity (a real [QualifiedName], for
 * structure references) to be compared for equality safely; [UnresolvedType]
 * may still be a bare, unqualified name.
 *
 * @see UnresolvedType
 */
sealed interface Type {

    /** @return A prepared string version of the type */
    fun asString() : String
}


// NOTE: NullType, VoidType, and AnyType are declared in UnresolvedType.kt,
//  implementing both interfaces: they carry no name-resolution dependency, so
//  the same instances are valid before and after resolution.


/**
 * UNKNOWN special type represents a variable which does not
 * have a value.
 *
 * It is linked to [drift.values.specials.NotAssignedValue].
 *
 * Unlike the shared leaves, [UnknownType] is never produced by the parser —
 * it is purely an inference-time marker for "this value's type could not be
 * determined" — so it only exists as a resolved [Type].
 *
 * @see Type
 */
data object UnknownType : Type {

    override fun asString(): String = "Unknown"
}



/**********************************
 * TYPE ENRICHING CONTAINERS
 **********************************/


/**
 * This type container adds the optional behavior
 * to the inner type. It allows using NULL as a value.
 *
 * @param inner Inner type to make optional
 * @see Type
 */
data class OptionalType(
    val inner: Type) : Type {

    override fun asString() = "${inner.asString()}?"
}


/**
 * This type container permits uniting provided
 * types. It allows typing an entity with many types.
 *
 * @param options United types (inner)
 * @see Type
 */
data class UnionType(
    val options: List<Type>) : Type {

    override fun asString() =
        options.joinToString(" | ") { it.asString() }
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
 * This includes every native structure — primitives, arrays, tuples, ranges —
 * each identified by its own [QualifiedName] under the `Homemade` module.
 *
 * @param qualifiedName Object's class qualified name
 * @param args Object arguments
 */
data class ObjectType(
    val qualifiedName: QualifiedName,
    val args: Map<String, TypeArgument> = emptyMap()) : Type {

    constructor(
        primitive: ParserPrimitiveClass,
        args: Map<String, TypeArgument> = emptyMap()) : this(primitive.qualifiedName, args)


    fun isPrimitiveNumeric() =
        isPrimitiveInt() || isPrimitiveInt64() || isPrimitiveUInt()

    fun isPrimitiveInt() = qualifiedName == ParserPrimitiveClass.Int.qualifiedName
    fun isPrimitiveInt64() = qualifiedName == ParserPrimitiveClass.Int64.qualifiedName
    fun isPrimitiveUInt() = qualifiedName == ParserPrimitiveClass.UInt.qualifiedName
    fun isPrimitiveString() = qualifiedName == ParserPrimitiveClass.String.qualifiedName
    fun isPrimitiveBool() = qualifiedName == ParserPrimitiveClass.Bool.qualifiedName
    fun isArray() = qualifiedName == ParserPrimitiveClass.Array.qualifiedName


    override fun asString() = qualifiedName.qualifiedName
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
    val paramTypes: List<Type> = emptyList(),
    val returnType: Type = AnyType) : Type {

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
 * @param qualifiedName The class's qualified name
 * @param generics
 */
data class ClassType(
    val qualifiedName: QualifiedName,
    val generics: Map<String, Type> = emptyMap()) : Type {

    override fun asString(): String =
        if (generics.isEmpty()) "Class<${qualifiedName.qualifiedName}>"
        else {
            val genericsAsString = generics.values
                .joinToString(", ") { it.asString() }

            "Class<${qualifiedName.qualifiedName}<$genericsAsString>>"
        }
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
fun isAssignable(valueType: Type, expected: Type): Boolean {
    if (valueType == UnknownType
        || expected == AnyType
        || expected == VoidType && valueType == VoidType) {

        return true
    }

    if (expected is ObjectType && valueType is ObjectType)
        return expected.qualifiedName == valueType.qualifiedName

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
