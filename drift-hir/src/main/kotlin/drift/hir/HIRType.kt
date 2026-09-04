/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import language.ModuleReference
import language.QualifiedName


/**
 * Base interface for all HIR types.
 * 
 * Types in HIR are backend-agnostic representations. Backends convert these
 * to their target type systems (e.g., QBEType, JVM type descriptors).
 */
sealed interface HIRType


/**
 * Primitive types available in Drift.
 */
enum class PrimitiveKind {
    INT,      // 32-bit signed integer
    INT64,    // 64-bit signed integer
    UINT,     // 32-bit unsigned integer
    BOOL,     // Boolean (true/false)
    STRING,   // String (unicode text)
    VOID,     // No return value / unit type
    NULL      // Null type
}


/**
 * A primitive type like Int, Bool, String, etc.
 */
data class HIRPrimitiveType(val kind: PrimitiveKind) : HIRType


/**
 * A class type reference (e.g., User, List<Int>).
 * 
 * @param qualifiedName The qualified name object of the class
 * @param typeArguments Generic type arguments (e.g., List -> {"elementType": Int})
 */
data class HIRClassType(
    val qualifiedName: QualifiedName,
    val typeArguments: Map<String, HIRType> = emptyMap()) : HIRType


/**
 * An optional type (nullable type).
 * 
 * Represents types that can be null (e.g., String?, Int?).
 */
data class HIROptionalType(val innerType: HIRType) : HIRType


/**
 * A union type (multiple possible types).
 * 
 * Represents values that can be one of several types (e.g., Int | String).
 */
data class HIRUnionType(val types: List<HIRType>) : HIRType


/**
 * A function type (for lambdas and function values).
 * 
 * @param parameterTypes Types of all parameters
 * @param returnType Type of the return value
 */
data class HIRFunctionType(
    val parameterTypes: List<HIRType>,
    val returnType: HIRType) : HIRType


/**
 * Any/unknown type.
 * 
 * Used when type information is unavailable or intentionally generic.
 */
object HIRAnyType : HIRType


/**
 * Helper function to convert a resolved [drift.types.Type] to an HIRType.
 * This is used during AST-to-HIR conversion.
 */
fun convertTypeToHIRType(type: drift.types.Type): HIRType {
    return when (type) {
        is drift.types.ObjectType -> when {
            type.isPrimitiveInt() -> HIRPrimitiveType(PrimitiveKind.INT)
            type.isPrimitiveInt64() -> HIRPrimitiveType(PrimitiveKind.INT64)
            type.isPrimitiveUInt() -> HIRPrimitiveType(PrimitiveKind.UINT)
            type.isPrimitiveBool() -> HIRPrimitiveType(PrimitiveKind.BOOL)
            type.isPrimitiveString() -> HIRPrimitiveType(PrimitiveKind.STRING)

            else -> {
                val args = type.args.mapValues { (_, argValue) ->
                    when (argValue) {
                        is drift.types.SingleType -> convertTypeToHIRType(argValue.type)
                        is drift.types.MultiTypes -> HIRClassType(
                            QualifiedName(module = ModuleReference.homemade, simpleName = "Tuple"),
                            argValue.types.mapIndexed { idx, t -> "$idx" to convertTypeToHIRType(t) }.toMap())

                        else -> HIRAnyType
                    }
                }

                HIRClassType(type.qualifiedName, args)
            }
        }
        is drift.types.FunctionType -> HIRFunctionType(
            parameterTypes = type.paramTypes.map { convertTypeToHIRType(it) },
            returnType = convertTypeToHIRType(type.returnType))
        is drift.types.ClassType -> HIRClassType(
            type.qualifiedName,
            type.generics.mapValues { (_, generic) -> convertTypeToHIRType(generic) })
        is drift.types.OptionalType -> HIROptionalType(convertTypeToHIRType(type.inner))
        is drift.types.UnionType -> HIRUnionType(type.options.map { convertTypeToHIRType(it) })
        is drift.types.VoidType -> HIRPrimitiveType(PrimitiveKind.VOID)
        is drift.types.NullType -> HIRPrimitiveType(PrimitiveKind.NULL)
        is drift.types.AnyType -> HIRAnyType
        is drift.types.UnknownType -> HIRAnyType
    }
}
