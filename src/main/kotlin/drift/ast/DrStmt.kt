package drift.ast

import drift.runtime.*

sealed interface DrStmt

data class ExprStmt(val expr: DrExpr) : DrStmt
data class Block(val statements: List<DrStmt>) : DrStmt
data class If(val condition: DrExpr, val thenBranch: DrStmt, val elseBranch: DrStmt?) : DrStmt
data class Return(val value: DrExpr) : DrStmt
data class Class(
    val name: String,
    val fields: MutableList<FunctionParameter> = mutableListOf(),
    val methods: MutableList<Function> = mutableListOf()) : DrStmt
data class Let(val name: String, val type: DrType, val value: DrExpr, val isMutable: Boolean) : DrStmt