package drift.ast

sealed interface DrStmt

data class ExprStmt(val expr: DrExpr) : DrStmt
data class Block(val statements: List<DrStmt>) : DrStmt
data class If(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrStmt