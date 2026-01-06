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
import drift.checkers.collectors.exceptions.DCAmbiguousMemberNameException
import drift.helper.validateValue
import drift.runtime.AnyType
import drift.runtime.DrEnv
import drift.runtime.evaluators.eval
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.oop.DrClass
import drift.runtime.values.specials.DrNotAssigned
import drift.runtime.values.variables.DrVariable
import drift.sslot.StaticSlot


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
            is Class ->
                ClassCollector().collectClass(stmt)

            is Function -> {
                val functionVar = DrVariable(
                    name = stmt.name,
                    type = AnyType,         // TODO: FunctionType ?
                    value = DrNotAssigned,
                    isMutable = false)

                env.define(stmt.name, functionVar)
            }

            is Let -> env.define(stmt.name, DrVariable(
                stmt.name,
                stmt.type,
                DrNotAssigned,
                stmt.isMutable))

            else -> {}
        }
    }



    /**
     * Class-specific collectors
     */
    internal inner class ClassCollector {

        /** Global member registry */
        val classStaticFields = mutableMapOf<String, StaticSlot>()
        val classMethods = mutableMapOf<String, DrMethod>()
        val classStaticMethods = mutableMapOf<String, DrMethod>()

        val members = mutableMapOf<String, MemberKind>()


        /**
         * Collect class' symbols: dynamic and static
         * fields and methods.
         *
         * Once the members are collected, the whole class
         * structure is defined in the current [DrEnv].
         *
         * @param stmt Class' declaration statement
         */
        internal fun collectClass(stmt: Class) {
            stmt.fields.forEach { field ->
                registerMember(field.name, MemberKind.FIELD)

                classFields[field.name] = field
            }

            stmt.staticFields.forEach { field ->
                registerMember(field.name, MemberKind.STATIC_FIELD)

                classStaticFields[field.name] = StaticSlot(
                    name = field.name,
                    type = field.type,
                    isMutable = field.isMutable,
                    initializer = { env -> validateValue(field.value.eval(env)) })
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

            val constructorType: DrClass.ConstructorType =
                if (stmt.hasPrimaryConstructor) DrClass.ConstructorType.PRIMARY
                else DrClass.ConstructorType.STANDARD

            env.defineClass(stmt.name, DrClass(
                stmt.name,
                classFields,
                classMethods,
                classStaticFields,
                classStaticMethods,
                env.copy(),
                constructorType))
        }


        /**
         * Register a class' member with its kind in
         * the global member registry.
         * It does not register to a dedicated registry
         * (e.g., fields one, etc.).
         *
         * It also verifies cases of ambiguous naming.
         *
         * @param name Class member's name
         * @param kind Class member's kind (type)
         */
        private fun registerMember(name: String, kind: MemberKind) {
            val memberKind = members[name]

            if (memberKind != null)
                throw DCAmbiguousMemberNameException(name, memberKind)

            members[name] = kind
        }
    }



    /**
     * Internal representation of a class member kind
     * (e.g., field, method, etc.).
     */
    enum class MemberKind(val label: String) {
        FIELD("field"),
        STATIC_FIELD("static field"),
        METHOD("method"),
        STATIC_METHOD("static method")
    }
}