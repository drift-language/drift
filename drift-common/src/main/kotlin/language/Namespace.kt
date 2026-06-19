/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package language

import language.LangInfo.NAMESPACE_SEPARATOR


/**
 * Representation of a namespace in the Drift Programming Language.
 * A namespace is a path that represents where a class or package can be located
 * inside the Drift virtual file system.
 *
 * In practice, the path is represented as a string separated by a separator
 * (cf. [NAMESPACE_SEPARATOR]).
 * 
 * @author Jonathan (GitHub: belicfr)
 * @see NAMESPACE_SEPARATOR
 */
data class Namespace(
    val namespace: String) {

    fun getFilename() : String =
        namespace.substringAfterLast(NAMESPACE_SEPARATOR)

    operator fun plus(other: String) = namespace + other

    override fun toString(): String = namespace
}