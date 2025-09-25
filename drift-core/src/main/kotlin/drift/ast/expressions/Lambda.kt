/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.ast.expressions

import drift.ast.statements.DrStmt
import drift.ast.statements.FunctionParameter
import drift.runtime.DrType


/******************************************************************************
 * DRIFT LAMBDA AST NODE
 *
 * Data class representing a lambda in an AST.
 ******************************************************************************/



/**
 * A lambda is a callable expression that can be stored
 * in a variable or returned by another function,
 * for example
 *
 * @property name Lambda entity name if defined (like variable)
 * @property parameters Lambda arguments structures
 * @property body Lambda body AST
 * @property returnType Lambda return type
 */
data class Lambda(
    val name: String? = null,
    val parameters: List<FunctionParameter>,
    val body: List<DrStmt>,
    val returnType: DrType) : Expression