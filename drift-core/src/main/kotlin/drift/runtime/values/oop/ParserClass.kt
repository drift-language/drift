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
import drift.runtime.ParserType
import drift.runtime.ParserValue
import drift.runtime.ObjectType
import drift.runtime.values.callables.ParserMethod
import drift.sslot.StaticSlot


/******************************************************************************
 * DRIFT CLASS RUNTIME TYPE
 *
 * Runtime class to represent Class structure.
 ******************************************************************************/


const val constructorName: String = "init"



/**
 * Runtime representation of a class.
 */
data class ParserClass(
    /** Class name */
    val name: String,

    /** Class fields, attributes */
    val fields: MutableMap<String, Let> = mutableMapOf(),

    /** Class methods */
    val methods: MutableMap<String, ParserMethod> = mutableMapOf(),

    /** Class static fields, attributes */
    val staticFields: MutableMap<String, StaticSlot> = mutableMapOf(),

    /** Class static methods */
    val staticMethods: MutableMap<String, ParserMethod> = mutableMapOf(),

    /** Class initialization environment */
    val closure: DrEnv,

    /** Class Constructor type (primary or standard) */
    val constructorType: ConstructorType? = null) : ParserValue {


    /** Constructor if existing, else NULL */
    val constructor : ParserMethod?
        get() = methods[constructorName]


    /** @return A prepared string version of the type */
    override fun asString() = "<[class#${hashCode()}] $name>"

    /** @return The object representation of the type */
    override fun type(): ParserType = ObjectType(name)


    enum class ConstructorType {
        PRIMARY,
        STANDARD
    }
}