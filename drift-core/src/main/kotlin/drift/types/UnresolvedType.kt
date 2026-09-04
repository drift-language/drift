/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.types


/******************************************************************************
 * DRIFT UNRESOLVED TYPES
 *
 * Type annotations exactly as the parser can produce them: names are not yet
 * resolved to a declaration (no module/namespace identity), and only the
 * shapes the current grammar can actually parse are represented here.
 ******************************************************************************/



/**
 * This interface represents every type shape the parser can produce from a
 * type annotation, before any name resolution has happened.
 *
 * **Attention!** [UnresolvedType] must not be confused with [Type]. A value of
 * [UnresolvedType] may still reference a structure by a bare, unqualified
 * name; only [Type] carries a real [language.QualifiedName].
 *
 * @see Type
 */
sealed interface UnresolvedType {

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
 * It carries no name-resolution dependency, so it is valid both before and
 * after resolution.
 *
 * @see UnresolvedType
 * @see Type
 */
data object NullType : UnresolvedType, Type {

    override fun asString(): String = "Null"
}



/**
 * VOID type represents the absence of return for a function.
 *
 * @see UnresolvedType
 * @see Type
 */
data object VoidType : UnresolvedType, Type {

    override fun asString(): String = "Void"
}



/**
 * ANY type represents the absence of type for a variable,
 * function return, parameter, etc.
 *
 * ANY is applied to any variable, function without explicit
 * type.
 *
 * @see UnresolvedType
 * @see Type
 */
data object AnyType : UnresolvedType, Type {

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
 * Unlike [NullType]/[VoidType]/[AnyType], [LastType] only ever appears as a
 * declared return-type annotation; it is replaced by the block's actual last
 * expression type during inference, so it never appears as a resolved [Type].
 *
 * @see UnresolvedType
 */
data object LastType : UnresolvedType {

    override fun asString(): String = "Last"
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
 * @see UnresolvedType
 */
data class UnresolvedOptional(
    val inner: UnresolvedType) : UnresolvedType {

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
 * @see UnresolvedType
 */
data class UnresolvedUnion(
    val options: List<UnresolvedType>) : UnresolvedType {

    override fun asString() =
        options.joinToString(" | ") { it.asString() }
}



/**********************************
 * OOP MAIN TYPES
 **********************************/


/**
 * This type represents a reference to a Drift object's class by a bare,
 * unqualified name, exactly as written in a type annotation.
 *
 * It is not yet resolved: [name] may refer to a class declared in the
 * current namespace, an imported one, or a native primitive — only
 * resolution (cf. [resolve]) can tell.
 *
 * @param name The bare, unqualified name as written in source.
 * @see UnresolvedType
 */
data class UnresolvedObjectType(
    val name: String) : UnresolvedType {

    override fun asString() = name
}
