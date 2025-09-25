/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.runtime.values.oop

import drift.ast.FunctionParameter
import drift.runtime.DrType
import drift.runtime.DrValue
import drift.runtime.ObjectType
import drift.runtime.values.callables.DrMethod
import drift.runtime.values.variables.DrVariable


/******************************************************************************
 * DRIFT CLASS RUNTIME TYPE
 *
 * Runtime class to represent Class structure.
 ******************************************************************************/



/**
 * AST representation of a class.
 */
data class DrClass(
    /** Class name */
    val name: String,

    /** Class fields, attributes */
    val fields: List<FunctionParameter>,

    /** Class methods */
    val methods: List<DrMethod>,

    /** Class static fields, attributes */
    val staticFields: MutableMap<String, DrVariable> = mutableMapOf(),

    /** Class static methods */
    val staticMethods: MutableMap<String, DrMethod> = mutableMapOf()) : DrValue {


    /** @return A prepared string version of the type */
    override fun asString() = "<[class#${hashCode()}] $name>"

    /** @return The object representation of the type */
    override fun type(): DrType = ObjectType(name)
}