/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.bootstrap


/**
 * This object hosts some collections caching compilation
 * useful data, accessible for all [Bootstrap] instances for
 * example.
 *
 * @author Jonathan (GitHub: belicfr)
 */
object CompilationMemory {

    /** This set stores all already imported modules. */
    val imported = mutableSetOf<String>()
}