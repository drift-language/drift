/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime

import drift.runtime.exceptions.DRAlreadyDefinedException
import drift.runtime.exceptions.DRClassAlreadyDefinedException
import drift.runtime.exceptions.DRClassNotDefinedException
import drift.runtime.exceptions.DRNotDefinedException
import drift.runtime.exceptions.DRUnsuccessfulCastException
import drift.runtime.exceptions.DRVariableAlreadyDefinedException
import drift.runtime.exceptions.DRVariableNotDefinedException
import drift.runtime.values.oop.DrClass
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * DRIFT TOP-LEVEL AND SCOPE ENVIRONMENT
 *
 * Top-level and each scope must have its own environment if required.
 * This file contains the Drift environment class.
 ******************************************************************************/



/**
 * Drift environment main class.
 *
 * Permits storing variables, functions and classes
 * on runtime.
 */
class DrEnv(
    /** Parent of the environment instance, if exists; else NULL */
    private val parent: DrEnv? = null,

    /** Environment's variables and functions */
    private val values: MutableMap<String, DrValue> = mutableMapOf(),

    /** Environment's classes */
    private val classes: MutableMap<String, DrClass> = mutableMapOf()) {



    /**
     * Define a variable or function into environment.
     *
     * @param name Entity name
     * @param value Entity value
     * @throws DRUnsuccessfulCastException
     * @throws DRAlreadyDefinedException
     */
    fun define(name: String, value: DrValue) {
        if (value is DrVariable && !isAssignable(value.value.type(), value.type()))
            throw DRUnsuccessfulCastException(value.value.type(), value.type())


        if (values.containsKey(name))
            throw DRAlreadyDefinedException(name = name)

        forceDefine(name, value)
    }



    /**
     * Force a variable or function definition into the environment.
     *
     * @param name Entity name
     * @param value Entity value
     */
    fun forceDefine(name: String, value: DrValue) {
        values[name] = value
    }



    /**
     * Define a class into the environment.
     *
     * @param name Class name
     * @param klass Class structure
     * @throws DRClassAlreadyDefinedException
     */
    fun defineClass(name: String, klass: DrClass) {
        if (classes.containsKey(name))
            throw DRClassAlreadyDefinedException(name = name)

        classes[name] = klass
    }



    /**
     * Assign another value to an existing variable or function.
     *
     * @param name Entity name
     * @param value New entity value
     * @throws DRVariableAlreadyDefinedException
     */
    fun assign(name: String, value: DrValue) {
        val variable = resolve(name) as? DrVariable
            ?: throw DRVariableNotDefinedException(name = name)

        variable.set(value)
    }



    /**
     * Assign another value to a class.
     *
     * @param name Class name
     * @param klass New class structure
     * @throws DRClassNotDefinedException
     */
    fun assignClass(name: String, klass: DrClass) {
        if (!classes.containsKey(name))
            throw DRClassNotDefinedException(name = name)

        classes[name] = klass
    }



    /**
     * @param name Entity name
     * @return If the provided entity name is already defined in the environment
     *         or one of its parents.
     */
    fun exists(name: String) : Boolean =
        values.containsKey(name) || parent?.exists(name)
            ?: false


    /**
     * Attempt to get a variable value or function structure,
     * by its name.
     *
     * @param name Entity name
     * @return Entity value
     * @throws DRNotDefinedException
     */
    fun get(name: String) : DrValue = values[name]
        ?: parent?.get(name)
        ?: throw DRNotDefinedException(name = name)



    /**
     * Get all recorded entities (variables values and
     * functions structures).
     *
     * @return Entities values/structures
     */
    fun all() : Map<String, DrValue> = values



    /**
     * Get all recorded classes structures.
     *
     * @return Classes structures
     */
    fun allClasses() : Set<String> {
        val aggregated = parent?.allClasses()?.toMutableSet()
            ?: mutableSetOf()
        aggregated.addAll(classes.keys)

        return aggregated
    }



    /**
     * Attempt to resolve an entity (variable or function)
     * by its name.
     *
     * Both current environment and parents values maps are used.
     *
     * @param name Entity name
     * @return Entity value/structure if exists; else, NULL
     */
    fun resolve(name: String) : DrValue? =
        values[name] ?: parent?.resolve(name)



    /**
     * Attempt to resolve a class by its name.
     *
     * Both current environment and parents classes maps are used.
     *
     * @param name Class name
     * @return Class structure if exists; else, NULL
     */
    fun resolveClass(name: String) : DrClass? =
        classes[name] ?: parent?.resolveClass(name)



    /**
     * Make a shallow copy of the environment.
     *
     * Maps will be cloned, but not their entities,
     * which keep their references.
     */
    fun copy() : DrEnv = DrEnv(
        parent,
        values.toMutableMap(),
        classes.toMutableMap())



    /**
     * @return If the environment has a parent one (not top-level),
     * or not
     */
    fun isTopLevel() : Boolean = parent == null



    /**
     * Export all environment recorded members and structures
     */
    fun export() : Map<String, DrValue> {
        return values + classes
    }
}