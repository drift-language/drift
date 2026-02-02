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
import drift.ast.statements.ParserStatement
import drift.ast.statements.Function
import drift.ast.statements.Let
import drift.checkers.collectors.exceptions.DCAmbiguousMemberNameException
import drift.helper.validateValue
import drift.runtime.AnyType
import drift.runtime.DrEnv
import drift.runtime.evaluators.eval
import drift.runtime.values.callables.ParserMethod
import drift.runtime.values.oop.ParserClass
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.variables.ParserVariable
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
    fun collect(ast: List<ParserStatement>) {
        for (stmt in ast) {
            collectStatement(stmt)
        }
    }



    /**
     * Pre-define found entities (classes, functions, variables)
     *
     * @param stmt Statement to check
     */
    private fun collectStatement(stmt: ParserStatement) {
        when (stmt) {
            is Class ->
                ClassCollector().collectClass(stmt)

            is Function -> {
                val functionVar = ParserVariable(
                    name = stmt.name,
                    type = AnyType,         // TODO: FunctionType ?
                    value = ParserNotAssigned,
                    isMutable = false)

                env.define(stmt.name, functionVar)
            }

            is Let -> env.define(stmt.name, ParserVariable(
                stmt.name,
                stmt.type,
                ParserNotAssigned,
                stmt.isMutable))

            else -> {}
        }
    }



    /**
     * Class-specific collectors
     */
    internal inner class ClassCollector {

        /** Global member registry */
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
            val fields = mutableMapOf<String, Let>()
            val staticFields = mutableMapOf<String, StaticSlot>()
            val methods = mutableMapOf<String, ParserMethod>()
            val staticMethods = mutableMapOf<String, ParserMethod>()
            val constructorType: ParserClass.ConstructorType =
                if (stmt.hasPrimaryConstructor) ParserClass.ConstructorType.PRIMARY
                else ParserClass.ConstructorType.STANDARD


            stmt.fields.forEach { field ->
                registerMember(field.name, MemberKind.FIELD)

                fields[field.name] = field
            }

            stmt.staticFields.forEach { field ->
                registerMember(field.name, MemberKind.STATIC_FIELD)

                staticFields[field.name] = StaticSlot(
                    name = field.name,
                    type = field.type,
                    isMutable = field.isMutable,
                    initializer = { env -> validateValue(field.value.eval(env)) })
            }

            stmt.methods.forEach { method ->
                registerMember(method.name, MemberKind.METHOD)

                methods[method.name] = ParserMethod(
                    method,
                    env.copy(),
                    null,
                    null)
            }

            stmt.staticMethods.forEach { method ->
                registerMember(method.name, MemberKind.STATIC_METHOD)

                staticMethods[method.name] = ParserMethod(
                    method,
                    env.copy(),
                    null,
                    null)
            }


            env.defineClass(stmt.name, ParserClass(
                stmt.name,
                fields,
                methods,
                staticFields,
                staticMethods,
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