package drift.ast

import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue

sealed interface DrExpr

data class Literal(val value: DrValue) : DrExpr
data class Variable(val name: String) : DrExpr
data class Call(val callee: DrExpr, val args: List<Argument>) : DrExpr
data class Binary(val left: DrExpr, val operator: String, val right: DrExpr) : DrExpr
data class Conditional(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrExpr
data class Lambda(val parameters: List<FunctionParameter>, val body: List<DrStmt>, val returnType: DrType) : DrExpr
data class Unary(val operator: String, val expr: DrExpr) : DrExpr

data class Argument(val name: String?, val expr: DrExpr)