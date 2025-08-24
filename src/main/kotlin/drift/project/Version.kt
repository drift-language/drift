/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.project


/******************************************************************************
 * DRIFT PROJECT VERSION HELPER
 ******************************************************************************/



/**
 * Drift Distribution Current Version
 */
val Project.version: String?
    get() = object {}.javaClass.getResource("/version.txt")?.readText()?.trim()