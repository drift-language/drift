/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.parser.types

import drift.parser.Parser
import drift.lexer.Token
import drift.parser.exceptions.DPSpecialInUnionTypeException
import drift.parser.exceptions.DPWrongOptionalUnionTypeException
import drift.runtime.*


/******************************************************************************
 * DRIFT TYPES PARSER METHODS
 *
 * All methods permitting parsing types are defined in this file.
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
 * @throws DPWrongOptionalUnionTypeException
 * @throws DPSpecialInUnionTypeException
 */
internal fun Parser.parseType() : ParserType {
    val types = mutableListOf<ParserType>()
    var foundOptional = false
    val token = expect<Token.Identifier>("type name")
    var type: ParserType = when (token.value) {
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
            throw DPWrongOptionalUnionTypeException()

        val next = parseType()
        when (next) {
            is LastType, is AnyType, is VoidType ->
                throw DPSpecialInUnionTypeException()
            is OptionalType ->
                throw DPWrongOptionalUnionTypeException()
            else ->
                types.add(next)
        }
    }

    return when (types.size) {
        0, 1 -> types.firstOrNull() ?: AnyType
        else -> UnionType(types)
    }
}