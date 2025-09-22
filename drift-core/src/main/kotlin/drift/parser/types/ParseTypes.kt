/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.types

import drift.exceptions.DriftParserException
import drift.exceptions.DriftTypeException
import drift.parser.Parser
import drift.parser.Token
import drift.runtime.*


/******************************************************************************
 * DRIFT TYPES PARSER METHODS
 *
 * All methods permitting to parse types are defined in this file.
 ******************************************************************************/



/**
 * Attempt to parse a type expression
 *
 * ```
 * let variable: Type
 * let optional: Type?
 * let union: Type|Type2
 * ```
 *
 * @return Constructed type AST object
 * @throws DriftParserException If both '?' and '|' operators
 * are used at the same time
 * @throws DriftTypeException If Last special type is united
 * to another type
 */
internal fun Parser.parseType() : DrType {
    val types = mutableListOf<DrType>()
    var foundOptional = false
    val token = expect<Token.Identifier>("Expected type name")
    var type: DrType = when (token.value) {
        "Null"      -> NullType
        "Void"      -> VoidType
        "Any"       -> AnyType
        "Last"      -> LastType
        else        -> ObjectType(token.value)
    }

    advance(false)

    if (matchSymbol("?")) {
        type = OptionalType(type)
        foundOptional = true
    }

    types.add(type)

    while (matchSymbol("|")) {
        if (foundOptional)
            throw DriftParserException("Cannot use both '?' and '|' in the same type declaration")

        val next = parseType()
        when (next) {
            is LastType, is AnyType, is VoidType ->
                throw DriftParserException("Cannot unite special type with another")
            is OptionalType ->
                throw DriftParserException("Cannot use both '?' and '|' in the same type declaration")
            else ->
                types.add(next)
        }
    }

    return when (types.size) {
        0, 1 -> types.firstOrNull() ?: AnyType
        else -> UnionType(types)
    }
}