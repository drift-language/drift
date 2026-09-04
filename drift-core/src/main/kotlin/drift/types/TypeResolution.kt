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
import language.ModuleReference
import language.Namespace
import language.QualifiedName


/**
 * Resolves an [UnresolvedType], as produced by the parser, into a [Type],
 * by qualifying every bare structure reference it contains with the
 * provided [module] and [namespace].
 *
 * A name matching a known [ParserPrimitiveClass] always resolves to that
 * primitive's real identity, regardless of [module]/[namespace]: primitives
 * are built-in and available everywhere, not scoped to whatever file
 * happens to reference them.
 *
 * [LastType] has no resolved counterpart: it must be special-cased by the
 * caller (replaced by whatever a function/lambda's block actually resolves
 * its last expression to) before this function is ever reached.
 *
 * @throws IllegalStateException If called with [LastType].
 */
fun UnresolvedType.resolve(module: ModuleReference, namespace: Namespace) : Type {
    return when (this) {
        is NullType -> NullType
        is VoidType -> VoidType
        is AnyType -> AnyType
        is LastType -> error("LastType has no resolved counterpart; it must be special-cased by the caller.")
        is UnresolvedObjectType -> {
            val primitive = ParserPrimitiveClass.entries.find { it.className == name }

            if (primitive != null) ObjectType(primitive)
            else ObjectType(QualifiedName(module = module, namespace = namespace, simpleName = name))
        }
        is UnresolvedOptional -> OptionalType(inner.resolve(module, namespace))
        is UnresolvedUnion -> UnionType(options.map { it.resolve(module, namespace) })
    }
}
