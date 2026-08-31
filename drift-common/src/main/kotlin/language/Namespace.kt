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
 * Representation of a namespace in Drift.
 * A namespace is a path that represents where a class or package can be located
 * inside the Drift virtual file system.
 *
 * This class stores its namespace using a list of steps. Each step represents
 * a package or class name, which composes the namespace.
 *
 * ``com.foo.bar`` using the Java package conventional naming;
 * becomes ``com/foo/bar`` using the internal Drift package conventional naming.
 * This class represents it as ``[ com, foo, bar ]``.
 *
 * Once formatted, the path is represented as a string separated by a separator
 * (cf. [NAMESPACE_SEPARATOR]).
 *
 * **Attention!** [Namespace] must not be confused with [QualifiedName].
 * A namespace can finish with a class name, like a qualified name, but for
 * composition purposes, to target a member.
 *
 * ``com/foo/bar/User`` targets the class ``User`` and can be used to target
 * its member``id`` by producing a [QualifiedName] object:
 * ```kotlin
 * QualifiedName(
 *     namespace = Namespace("com", "foo", "bar", "User"),
 *     simpleName = "id")
 * // It qualifies the 'id' member using a namespace containing the class's
 * // name.
 * ```
 * 
 * @author Jonathan (GitHub: belicfr)
 * @see QualifiedName
 */
data class Namespace(
    private val steps: List<String>) {

    companion object {

        val empty = Namespace()
    }


    constructor(vararg steps: String) : this(steps.toList())

    constructor(qualifiedName: QualifiedName) : this(
        qualifiedName.qualifiedName.split(NAMESPACE_SEPARATOR))


    /**
     * Since [Namespace] stores its namespace as a [List] of [String],
     * [getNamespace] permits preparing it as a [String].
     *
     * @return string version of the namespace's steps using
     *         [NAMESPACE_SEPARATOR].
     */
    fun getNamespace() : String = steps.joinToString(NAMESPACE_SEPARATOR)

    /**
     * Returns the namespace object as a string, joined using the '/' symbol.
     * This method does not depend on [NAMESPACE_SEPARATOR].
     *
     * @return the path string version of the current namespace instance.
     */
    fun toPath() : String = steps.joinToString("/")

    /**
     * Returns the last part of the [steps] list if non-empty; else it throws
     * an exception: an empty namespace cannot be decomposed and contain a
     * simple name.
     *
     * @return The namespace's simple name.
     */
    fun last() : String =
        if (isEmpty()) error("An empty namespace cannot be decomposed.")
        else steps.last()

    /**
     * Returns the [steps] list after dropping its last step if non-empty;
     * else it throws an exception: an empty namespace cannot be decomposed and
     * have a parent.
     *
     * @return The namespace's parent [Namespace] object.
     */
    fun parent() : Namespace =
        if (isEmpty()) error("An empty namespace cannot be decomposed.")
        else Namespace(steps.dropLast(1))

    /**
     * Returns the [steps] list after dropping its last step if non-empty;
     * else it returns the [empty] instance.
     *
     * @return The namespace's parent [Namespace] object if possible or [empty].
     */
    fun parentOrEmpty() : Namespace =
        if (isEmpty()) empty
        else parent()

    /**
     * Returns a new object containing the current [steps] list appened by the
     * provided new step.
     *
     * @param step The new step to append.
     * @return The new [Namespace] containing the new steps list.
     */
    fun addStep(step: String) : Namespace = Namespace(steps + step)

    fun isEmpty() = steps.isEmpty()


    operator fun plus(other: String) = addStep(other)


    override fun toString(): String = getNamespace()
}