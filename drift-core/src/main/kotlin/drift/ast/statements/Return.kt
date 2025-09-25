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
 * DRIFT RETURN STATEMENT AST NODE
 *
 * Data class representing a return statement in an AST.
 ******************************************************************************/



/**
 * This class represents a callable return statement
 *
 * @property value Value to return from the callable
 */
data class Return(
    val value: Expression) : DrStmt