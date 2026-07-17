/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package language


/**
 *
 * 
 * @author Jonathan (GitHub: belicfr)
 */
data class QualifiedName(
    val namespace: Namespace,
    val simpleName: String) {

    val qualifiedName: String
        get() = namespace.addStep(simpleName).getQualifiedName()

    override fun toString(): String = qualifiedName
}
