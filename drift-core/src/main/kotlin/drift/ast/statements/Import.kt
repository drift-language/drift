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
 * DRIFT IMPORT STATEMENT AST NODE
 *
 * Data class representing an import statement in an AST.
 ******************************************************************************/



/**
 * This class represents an import statement.
 *
 * @property namespace Namespace to import
 * @property path Namespace path to import
 */
data class Import(
    val namespace: String,
    val steps: List<String>) : DrStmt