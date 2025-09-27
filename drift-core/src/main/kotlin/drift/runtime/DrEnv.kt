/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime

import drift.exceptions.DriftRuntimeException
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
 * Permits to store variables, functions and classes
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
     * @throws DriftRuntimeException If the provided name is already used
     * into entities map.
     */
    fun define(name: String, value: DrValue) {
        if (values.containsKey(name))
            throw DriftRuntimeException("Entity '$name' is already defined")

        forceDefine(name, value)
    }



    /**
     * Force a variable or function definition into environment.
     *
     * @param name Entity name
     * @param value Entity value
     */
    fun forceDefine(name: String, value: DrValue) {
        values[name] = value
    }



    /**
     * Define a class into environment.
     *
     * @param name Class name
     * @param klass Class structure
     * @throws DriftRuntimeException If the provided name is already used
     * into classes map.
     */
    fun defineClass(name: String, klass: DrClass) {
        if (classes.containsKey(name))
            throw DriftRuntimeException("Class '$name' is already defined")

        classes[name] = klass
    }



    /**
     * Assign another value to an existing variable or function.
     *
     * @param name Entity name
     * @param value New entity value
     * @throws DriftRuntimeException If the provided name is no longer used
     * by the values map of the current environment and its parents
     */
    fun assign(name: String, value: DrValue) {
        val variable: DrVariable = resolve(name) as? DrVariable
            ?: throw DriftRuntimeException("Variable '$name' does not exist")

        variable.set(value)
    }



    /**
     * Assign another value to a class.
     *
     * @param name Class name
     * @param klass New class structure
     * @throws DriftRuntimeException If the provided name is no longer used
     * by the classes map of the current environment and its parents
     */
    fun assignClass(name: String, klass: DrClass) {
        if (!classes.containsKey(name))
            throw DriftRuntimeException("Class '$name' does not exist")

        classes[name] = klass
    }



    /**
     * Attempt to get a variable value or function structure,
     * by its name.
     *
     * @param name Entity name
     * @return Entity value
     * @throws DriftRuntimeException If the provided name is no
     * longer recorded into values map
     */
    fun get(name: String) : DrValue = values[name]
        ?: parent?.get(name)
        ?: throw DriftRuntimeException("Undefined symbol: $name")



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