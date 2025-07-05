package drift.check

import drift.ast.*
import drift.exceptions.DriftSemanticException
import drift.runtime.*

class TypeChecker (private val env: DrEnv) {
    fun check(ast: List<DrStmt>) {
        for (stmt in ast) {
            checkStatement(stmt)
        }
    }

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
            is Ternary -> {
                checkExpr(expr.condition)
                checkExpr(expr.thenBranch)
                expr.elseBranch?.let { checkExpr(it) }
            }
            else -> {}
        }
    }

    private fun checkType(type: DrType) {
        when (type) {
            is ClassType -> if (env.resolveClass(type.name) == null)
                throw DriftSemanticException("Unknown class type '${type.name}'")
            is OptionalType -> checkType(type.inner)
            is UnionType -> type.options.forEach { checkType(it) }
            is FunctionType -> {
                type.paramTypes.forEach { checkType(it) }
                checkType(type.returnType)
            }
            else -> {}
        }
    }
}