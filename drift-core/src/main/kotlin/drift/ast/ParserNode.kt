/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast


/** An [Int] that represents a node ID. */
@JvmInline
value class NodeId(val value: Int) {

    operator fun inc() = NodeId(value + 1)
}


abstract class ParserNode {

    val nodeId: NodeId = AstNodeId.allocate()
}