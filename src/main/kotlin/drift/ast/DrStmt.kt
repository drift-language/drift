package drift.ast

import drift.runtime.DrValue

sealed interface DrStmt

data class ExprStmt(val expr: DrExpr) : DrStmt
data class Block(val statements: List<DrStmt>) : DrStmt
data class If(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrStmt
data class Return(val value: DrExpr) : DrStmt

class ReturnException(val value: DrValue) : RuntimeException()