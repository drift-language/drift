/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.checkers.collectors

import drift.ast.statements.Class
import drift.ast.statements.DrStmt
import drift.ast.statements.Function
import drift.ast.statements.Let
import drift.exceptions.DriftParserException
import drift.runtime.DrEnv
import drift.runtime.values.callables.DrFunction
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.variables.DrVariable

/******************************************************************************
 * DRIFT SYMBOL COLLECTOR CHECKER
 *
 * The symbol collector is a middleware that pre-defines each
 * entity before run the code
 ******************************************************************************/


/**
 * The symbol collector is a middleware that pre-defines each
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
            is Class -> ClassCollector().collectClass(stmt)
            is Function -> {
                val function = DrFunction(stmt, env.copy())
                env.define(stmt.name, function)
            }
            is Let ->
                env.define(stmt.name, DrVariable(stmt.name, stmt.type, DrNotAssigned, stmt.isMutable))
            else -> {}
        }
    }



    /**
     * Class-specific collectors
     */
    internal inner class ClassCollector {

        val classFields = mutableMapOf<String, DrVariable>()
        val classStaticFields = mutableMapOf<String, DrVariable>()
        val classMethods = mutableMapOf<String, DrMethod>()
        val classStaticMethods = mutableMapOf<String, DrMethod>()

        val members = mutableMapOf<String, MemberKind>()

        internal fun collectClass(stmt: Class) {
            stmt.fields.forEach { field ->
                registerMember(field.name, MemberKind.FIELD)

                classFields[field.name] = convertLetToVariable(field)
            }

            stmt.staticFields.forEach { field ->
                registerMember(field.name, MemberKind.STATIC_FIELD)

                classStaticFields[field.name] = convertLetToVariable(field)
            }

            stmt.methods.forEach { method ->
                registerMember(method.name, MemberKind.METHOD)

                classMethods[method.name] = DrMethod(
                    method,
                    env.copy(),
                    null,
                    null)
            }

            stmt.staticMethods.forEach { method ->
                registerMember(method.name, MemberKind.STATIC_METHOD)

                classStaticMethods[method.name] = DrMethod(
                    method,
                    env.copy(),
                    null,
                    null)
            }

            env.defineClass(stmt.name, DrClass(
                stmt.name,
                classFields,
                classMethods,
                classStaticFields,
                classStaticMethods))
        }


        private fun registerMember(name: String, kind: MemberKind) {
            val memberKind = members[name]

            if (memberKind != null)
                throw DriftParserException("Ambiguous member name: $name is already defined as $memberKind")

            members[name] = kind
        }
    }
}


internal fun convertLetToVariable(let: Let) : DrVariable =
    DrVariable(
        let.name,
        let.type,
        DrNotAssigned,
        let.isMutable
    )


internal enum class MemberKind {
    FIELD,
    STATIC_FIELD,
    METHOD,
    STATIC_METHOD
}