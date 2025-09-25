/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.statements

import drift.ast.expressions.Expression


/******************************************************************************
 * DRIFT FOR-LOOP STATEMENT AST NODE
 *
 * Data class representing a for-loop in an AST.
 ******************************************************************************/



/**
 * This class represents a for loop statement structure
 *
 * @property iterable Source to iterate
 * @property variables List of loop variables
 * @property body Loop body block
 */
data class For(
    val iterable: Expression,
    val variables: List<String>,
    val body: DrStmt) : DrStmt