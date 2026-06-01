/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements

import drift.ast.metadata.Annotation
import drift.ast.statements.hooks.ParserHook


/******************************************************************************
 * DRIFT CLASS DECLARATION STATEMENT AST NODE
 *
 * Data class representing a class declaration in an AST.
 ******************************************************************************/



/**
 * This represents a Drift class declaration
 *
 * @property name Class name
 * @property fields Class fields
 * @property methods Class methods
 */
data class Class(
    val name: String,
    val annotations: MutableList<Annotation> = mutableListOf(),
    val fields: MutableList<Let> = mutableListOf(),
    val methods: MutableList<Func> = mutableListOf(),
    val hooks: MutableList<ParserHook> = mutableListOf(),
    val staticFields: MutableList<Let> = mutableListOf(),
    val staticMethods: MutableList<Func> = mutableListOf(),
    val hasPrimaryConstructor: Boolean = false)
    : ParserStatement() {

    fun hookExists(name: String) =
        hooks.firstOrNull { it.name == name } != null
}