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
@JvmInline value class ModuleReference(
    val name: String) {

    companion object {

        /** Module reference for structures synthesized by the compiler itself. */
        val homemade = ModuleReference("Homemade")

        /**
         * Placeholder module reference for user-declared structures, used
         * until real module resolution (from `drift.json`) is threaded
         * through the analysis pipeline.
         *
         * TODO: replace usages of [unresolved] once module resolution is
         *  wired into the pipeline.
         */
        val unresolved = ModuleReference("Unknown")
    }
}