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
    val token = expect<Token.Identifier>("Expected type name")
    val type: DrType = when (token.value) {
        "Int"       -> ObjectType("Int")
        "String"    -> ObjectType("String")
        "Bool"      -> ObjectType("Bool")
        "Null"      -> NullType
        "Void"      -> VoidType
        "Any"       -> AnyType
        "Last"      -> LastType
        else        -> ClassType(token.value)
    }

    advance(ignoreNewLines = false, ignoreWhitespaces = !peekSymbol("?"))

    val isOptional = matchSymbol("?")
    val left: DrType =
        if (isOptional) OptionalType(type)
        else type

    if (isOptional && checkSymbol("|")) {
        throw DriftParserException("Cannot use both '?' and '|' in the same type declaration")
    }

    val unionTypes: MutableList<DrType> = mutableListOf(left)

    while (matchSymbol("|")) {
        val next = parseType()

        if (next is LastType)
            throw DriftTypeException("Cannot unite Last type")

        unionTypes.add(next)
    }

    return if (unionTypes.size == 1) {
        unionTypes[0]
    } else {
        UnionType(unionTypes)
    }
}