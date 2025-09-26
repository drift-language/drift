/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements


/******************************************************************************
 * DRIFT IMPORT STATEMENT PART AST NODE
 *
 * Data class representing an import part in an AST.
 ******************************************************************************/



/**
 * Part of an import statement.
 *
 * ```
 * import drift.source { MyClass as IncredibleClass }
 * ```
 */
data class ImportPart(
    val source: String,
    val alias: String? = null)
