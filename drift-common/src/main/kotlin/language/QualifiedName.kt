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
 * Representation of a qualified name in Drift.
 * The name qualification consists of including the namespace with the
 * structure's name.
 *
 * Example:
 * - A namespace ``com/foo/bar``
 * - A class named ``User``
 *
 * The qualified name would be ``com/foo/bar/User``.
 *
 * This system permits avoiding ambiguity between many structures that have the
 * same name.
 *
 * **Attention!** [QualifiedName] must not be confused with [Namespace].
 * A qualified name finishes with a structure name, a namespace can finish with
 * a class name too, but for composition purposes
 * (cf. [Namespace] documentation).
 *
 * @param namespace A qualified name takes a namespace which starts it. This
 *                  namespace can be empty only for structures whose file has no
 *                  package, living at the project's source root directory.
 * @param simpleName The structure's name.
 * 
 * @author Jonathan (GitHub: belicfr)
 */
data class QualifiedName(
    val namespace: Namespace,
    val simpleName: String) {

    /**
     * This constructor permits building a [QualifiedName] object by decomposing
     * a [Namespace] one.
     *
     * In this case, [QualifiedName.namespace] must contain the parent
     * namespace: for example, the parent of ``com/foo/bar`` is ``com/foo``.
     * [QualifiedName.simpleName] must contain the provided namespace's one,
     * which is the last part, absent from the parent namespace.
     *
     * @param namespace The namespace object to use to construct the
     *                  [QualifiedName] object.
     * @throws IllegalStateException If the provided namespace is empty: it
     *                               cannot have neither a parent nor a simple
     *                               name.
     *   TODO DOC: replace IllegalStateException by a dedicated exception class.
     */
    constructor(namespace: Namespace) : this(
        namespace = namespace.getParent(),
        simpleName = namespace.getSimpleName())


    /** A string version of the current qualified name object. */
    val qualifiedName: String
        get() = (namespace + simpleName).getNamespace()

    override fun toString(): String = qualifiedName
}
