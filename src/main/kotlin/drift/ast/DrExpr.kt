package drift.ast

import drift.runtime.DrEnv
import drift.runtime.DrValue

sealed interface DrExpr

data class Literal(val value: DrValue) : DrExpr
data class Variable(val name: String) : DrExpr
data class Call(val callee: DrExpr, val args: List<DrExpr>) : DrExpr
data class Binary(val left: DrExpr, val operator: String, val right: DrExpr) : DrExpr
data class Conditional(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrExpr