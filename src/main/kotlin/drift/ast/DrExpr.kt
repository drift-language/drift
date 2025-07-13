/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast

import drift.runtime.DrType
import drift.runtime.DrValue


/******************************************************************************
 * DRIFT EXPRESSION STRUCTURES
 *
 * All Drift expression structures are defined in this file.
 ******************************************************************************/



/**
 * This interface represents all expression
 * structures
 */
sealed interface DrExpr



/**
 * A literal expression directly contains a value
 *
 * @property value Literal value
 */
data class Literal(val value: DrValue) : DrExpr



/**
 * A variable is represented by a name
 *
 * @property name Variable name
 */
data class Variable(val name: String) : DrExpr



/**
 * This class represents a callable call
 *
 * @property callee Callable name
 * @property args Callable arguments list
 */
data class Call(val callee: DrExpr, val args: List<Argument>) : DrExpr



/**
 * A binary structure represents an operation
 * with two operands and an operator
 *
 * @property left Left operand
 * @property operator Operator
 * @property right Right operand
 */
data class Binary(val left: DrExpr, val operator: String, val right: DrExpr) : DrExpr



/**
 * A ternary conditional structure contains a condition,
 * a then and else branches.
 *
 * It is the recommended syntax for conditional evaluation,
 * instead of [If].
 *
 * @property condition Condition
 * @property thenBranch Branch to execute if the
 * condition is successful
 * @property elseBranch Branch to execute if the
 * condition is unsuccessful
 */
data class Conditional(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrExpr



/**
 * A lambda is a callable expression that can be stored
 * in a variable, or returned by another function,
 * for example
 *
 * @property name Lambda entity name if defined (like variable)
 * @property parameters Lambda arguments structures
 * @property body Lambda body AST
 * @property returnType Lambda return type
 */
data class Lambda(
    val name: String? = null, 
    val parameters: List<FunctionParameter>, 
    val body: List<DrStmt>, 
    val returnType: DrType) : DrExpr



/**
 * A unary expression represents an expression
 * with an operator which is applied to once
 * operand
 *
 * @property operator Unary operator
 * @property expr Expression
 */
data class Unary(val operator: String, val expr: DrExpr) : DrExpr



/**
 * An assign structure contains the variable name
 * and value
 *
 * @property name Variable name
 * @property value Value to assign
 */
data class Assign(val name: String, val value: DrExpr) : DrExpr



/**
 * A get structure represents an object field access
 *
 * @property receiver Object where the field is defined
 * @property name Field name to retrieve
 */
data class Get(val receiver: DrExpr, val name: String) : DrExpr



/**
 * A set structure represents an object field assignment
 *
 * @property receiver Object where the field is defined
 * @property name Field name to assign
 * @property value Value to assign
 */
data class Set(val receiver: DrExpr, val name: String, val value: DrExpr) : DrExpr



/**
 * A list structure
 *
 * @property values List values
 */
data class ListLiteral(val values: MutableList<DrExpr>) : DrExpr



/**
 * An argument structure represent a call argument
 *
 * @property name Argument name
 * @property expr Argument value expression
 */
data class Argument(val name: String?, val expr: DrExpr)