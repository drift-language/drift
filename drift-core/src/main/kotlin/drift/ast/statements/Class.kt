/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements


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
    val fields: MutableList<FunctionParameter> = mutableListOf(),
    val methods: MutableList<Function> = mutableListOf(),
    val staticFields: MutableList<FunctionParameter> = mutableListOf(),
    val staticMethods: MutableList<Function> = mutableListOf()) : DrStmt