/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.checkers

import drift.ast.expressions.Assign
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.ParserExpression
import drift.ast.expressions.Get
import drift.ast.expressions.Set
import drift.ast.expressions.Unary
import drift.ast.statements.Block
import drift.ast.statements.ParserStatement
import drift.ast.statements.ExprStmt
import drift.ast.statements.If
import drift.ast.statements.Let
import drift.ast.statements.Return
import drift.runtime.*
import drift.runtime.exceptions.DRClassNotDefinedException


/******************************************************************************
 * DRIFT TYPE CHECKER
 *
 * The type checker is a middleware that validate entities types.
 ******************************************************************************/



/**
 * The type checker is a middleware that validate entities types.
 *
 * @property env Environment instance to use
 */
class TypeChecker (private val env: DrEnv) {


    /**
     * Start to check the provided AST,
     * by checking each statement
     *
     * @param ast AST to check
     */
    fun check(ast: List<ParserStatement>) {
        for (stmt in ast) {
            checkStatement(stmt)
        }
    }



    /**
     * Check the type of each statement expressions
     *
     * @param stmt Statement to check
     */
    private fun checkStatement(stmt: ParserStatement) {
        when (stmt) {
            is Let -> checkType(stmt.type)
            is ExprStmt -> checkExpr(stmt.expr)
            is Return -> checkExpr(stmt.value)
            is Block -> stmt.statements.forEach { checkStatement(it) }
            is If -> {
                checkExpr(stmt.condition)
                checkStatement(stmt.thenBranch)
                stmt.elseBranch?.let { checkStatement(it) }
            }
            else -> {}
        }
    }



    /**
     * Check te type of the expression and
     * its components
     *
     * @param expr Expression to check
     */
    private fun checkExpr(expr: ParserExpression) {
        when (expr) {
            is Binary -> {
                checkExpr(expr.left)
                checkExpr(expr.right)
            }
            is Unary -> checkExpr(expr.expr)
            is Call -> {
                checkExpr(expr.callee)
                expr.args.forEach { checkExpr(it.expr) }
            }
            is Assign -> checkExpr(expr.value)
            is Set -> {
                checkExpr(expr.receiver)
                checkExpr(expr.value)
            }
            is Get -> checkExpr(expr.receiver)
            else -> {}
        }
    }



    /**
     * Attempt to check the provided type
     *
     * @param type Type to check
     */
    private fun checkType(type: ParserType) {
        when (type) {
            is OptionalType -> checkType(type.inner)
            is UnionType -> type.options.forEach { checkType(it) }
            is ObjectType -> env.resolveClass(type.className)
                ?: throw DRClassNotDefinedException(name = type.className)
            else -> {}
        }
    }
}