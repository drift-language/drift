package drift.checkers

import drift.ast.*
import drift.ast.Function
import drift.runtime.*
import drift.runtime.values.callables.DrFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.variables.DrVariable

class SymbolCollector(private val env: DrEnv) {
    fun collect(ast: List<DrStmt>) {
        for (stmt in ast) {
            collectStatement(stmt)
        }
    }

    private fun collectStatement(stmt: DrStmt) {
        when (stmt) {
            is Class ->
                env.defineClass(stmt.name, DrClass(stmt.name, stmt.fields, emptyList()))
            is Function -> {
                val function = DrFunction(stmt, env.copy())
                env.define(stmt.name, function)
            }
            is Let ->
                env.define(stmt.name, DrVariable(stmt.name, stmt.type, DrNotAssigned, stmt.isMutable))
            else -> {}
        }
    }
}