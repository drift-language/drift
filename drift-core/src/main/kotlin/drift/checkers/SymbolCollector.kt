/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.checkers

import drift.ast.statements.Function
import drift.ast.statements.Class
import drift.ast.statements.DrStmt
import drift.ast.statements.Let
import drift.runtime.*
import drift.runtime.values.callables.DrFunction
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * DRIFT SYMBOL COLLECTOR CHECKER
 *
 * The symbol collector is a middleware that pre-define each
 * entity before run the code
 ******************************************************************************/



/**
 * The symbol collector is a middleware that pre-define each
 * entity before run the code
 *
 * @property env Environment instance to use
 */
class SymbolCollector(private val env: DrEnv) {


    /**
     * Start to collect from the provided AST
     * by checking each statement
     *
     * @param ast AST to check
     */
    fun collect(ast: List<DrStmt>) {
        for (stmt in ast) {
            collectStatement(stmt)
        }
    }



    /**
     * Pre-define found entities (classes, functions, variables)
     *
     * @param stmt Statement to check
     */
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