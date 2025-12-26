/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.oop

import drift.ast.statements.Let
import drift.runtime.DrEnv
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.values.callables.DrMethod
import drift.sslot.StaticSlot


/******************************************************************************
 * DRIFT CLASS RUNTIME TYPE
 *
 * Runtime class to represent Class structure.
 ******************************************************************************/


const val constructorName: String = "init"



/**
 * AST representation of a class.
 */
data class DrClass(
    /** Class name */
    val name: String,

    /** Class fields, attributes */
    val fields: MutableMap<String, Let> = mutableMapOf(),

    /** Class methods */
    val methods: MutableMap<String, DrMethod> = mutableMapOf(),

    /** Class static fields, attributes */
    val staticFields: MutableMap<String, StaticSlot> = mutableMapOf(),

    /** Class static methods */
    val staticMethods: MutableMap<String, DrMethod> = mutableMapOf(),

    /** Class initialization environment */
    val closure: DrEnv,

    /** Class Constructor type (primary or standard) */
    val constructorType: ConstructorType? = null) : DrValue {


    /** Constructor if existing, else NULL */
    val constructor : DrMethod?
        get() = methods[constructorName]


    /** @return A prepared string version of the type */
    override fun asString() = "<[class#${hashCode()}] $name>"

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(name)


    enum class ConstructorType {
        PRIMARY,
        STANDARD
    }
}