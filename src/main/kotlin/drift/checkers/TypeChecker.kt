/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.checkers

import drift.ast.*
import drift.exceptions.DriftSemanticException
import drift.runtime.*


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
    fun check(ast: List<DrStmt>) {
        for (stmt in ast) {
            checkStatement(stmt)
        }
    }



    /**
     * Check the type of each statement expressions
     *
     * @param stmt Statement to check
     */
    private fun checkStatement(stmt: DrStmt) {
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
    private fun checkExpr(expr: DrExpr) {
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
            is drift.ast.Set -> {
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
     * @throws DriftSemanticException If a class is
     * no longer defined on checking
     */
    private fun checkType(type: DrType) {
        when (type) {
            is OptionalType -> checkType(type.inner)
            is UnionType -> type.options.forEach { checkType(it) }
            else -> {}
        }
    }
}