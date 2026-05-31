/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

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
 * @param className The name of the class
 * @param typeArguments Generic type arguments (e.g., List -> {"elementType": Int})
 */
data class HIRClassType(
    val className: String,
    val typeArguments: Map<String, HIRType> = emptyMap()
) : HIRType

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
 * Helper function to convert a ParserType to an HIRType.
 * This is used during AST-to-HIR conversion.
 */
fun convertParserTypeToHIRType(parserType: drift.runtime.ParserType): HIRType {
    return when (parserType) {
        is drift.runtime.ObjectType -> when (parserType.className) {
            "Int" -> HIRPrimitiveType(PrimitiveKind.INT)
            "Int64" -> HIRPrimitiveType(PrimitiveKind.INT64)
            "UInt" -> HIRPrimitiveType(PrimitiveKind.UINT)
            "Bool" -> HIRPrimitiveType(PrimitiveKind.BOOL)
            "String" -> HIRPrimitiveType(PrimitiveKind.STRING)

            else -> {
                val args = parserType.args.mapValues { (_, argValue) ->
                    when (argValue) {
                        is drift.runtime.SingleType -> convertParserTypeToHIRType(argValue.type)
                        is drift.runtime.MultiTypes -> HIRClassType("Tuple",
                            argValue.types.mapIndexed { idx, t -> "$idx" to convertParserTypeToHIRType(t) }.toMap())
                        else -> HIRAnyType
                    }
                }
                HIRClassType(parserType.className, args)
            }
        }
        is drift.runtime.FunctionType -> HIRFunctionType(
            parameterTypes = parserType.paramTypes.map { convertParserTypeToHIRType(it) },
            returnType = convertParserTypeToHIRType(parserType.returnType))
        is drift.runtime.OptionalType -> HIROptionalType(convertParserTypeToHIRType(parserType.inner))
        is drift.runtime.UnionType -> HIRUnionType(parserType.options.map { convertParserTypeToHIRType(it) })
        is drift.runtime.VoidType -> HIRPrimitiveType(PrimitiveKind.VOID)
        is drift.runtime.NullType -> HIRPrimitiveType(PrimitiveKind.NULL)
        is drift.runtime.AnyType -> HIRAnyType

        else -> HIRAnyType
    }
}
