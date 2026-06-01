/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import drift.hir.metadata.HIRAnnotation


/**
 * Base interface for all HIR (High-level Intermediate Representation) nodes.
 * 
 * Every HIR node has a unique identifier for tracking and reference purposes.
 */
sealed interface HIRNode {
    val hirId: Int
}


/**
 * Base interface for all statement nodes in HIR.
 * 
 * Statements represent actions and control flow but don't produce values
 * (except ExprStmt, which wraps expressions).
 */
sealed interface HIRStatement : HIRNode

sealed interface HIRAnnotatable {

    val annotations: MutableList<HIRAnnotation>
}


/**
 * Base interface for all expression nodes in HIR.
 * 
 * Every expression has a resolved type that's embedded in the node.
 * This makes HIR self-contained - backends don't need external type lookups.
 */
sealed interface HIRExpression : HIRNode {
    val type: HIRType
}
