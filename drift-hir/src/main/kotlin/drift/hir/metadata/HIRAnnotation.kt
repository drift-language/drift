/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.hir.metadata

import drift.hir.HIRArgument

data class HIRAnnotation(
    val name: String,
    val args: List<HIRArgument> = listOf())