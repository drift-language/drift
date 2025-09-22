/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast

import drift.runtime.*


/******************************************************************************
 * DRIFT STATEMENT STRUCTURES
 *
 * All Drift statement structures are defined in this file.
 ******************************************************************************/



/**
 * This interface represents all statement
 * structures.
 */
sealed interface DrStmt



/**
 * This class represents a statement exclusively
 * composed by an expression, like a function call.
 *
 * ```
 * call()  // This is a expression statement
 * ```
 *
 * @property expr Statement expression
 */
data class ExprStmt(val expr: DrExpr) : DrStmt



/**
 * A block is a statement container allowing
 * to compute statements into an isolated environment
 * instance, inherited by parents.
 *
 * @property statements Block statements
 */
data class Block(val statements: List<DrStmt>) : DrStmt



/**
 * This class represents a classic conditional statement.
 *
 * The Drift-style structure is an expression,
 * and is computed using [ExprStmt].
 *
 * @property condition Condition to compute
 * @property thenBranch Branch to execute if the
 * condition is successful
 * @property elseBranch Branch to execute if the
 * condition is unsuccessful
 */
data class If(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrStmt



/**
 * This class represents a callable return statement
 *
 * @property value Value to return from the callable
 */
data class Return(val value: DrExpr) : DrStmt



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



/**
 * This class represents a variable declaration
 *
 * @property name Variable name
 * @property type Variable type
 * @property value Variable value
 * @property isMutable If the variable is mutable, can be reassigned
 */
data class Let(val name: String, val type: DrType, val value: DrExpr, val isMutable: Boolean) : DrStmt



/**
 * This class represents a for loop statement structure
 *
 * @property iterable Source to iterate
 * @property variables List of loop variables
 * @property body Loop body block
 */
data class For(val iterable: DrExpr, val variables: List<String>, val body: DrStmt) : DrStmt



/**
 * This class represents an import statement.
 *
 * @property namespace Namespace to import
 * @property path Namespace path to import
 */
data class Import(val namespace: String, val path: String) : DrStmt