/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.ast.statements

import drift.ast.AstNodeId
import drift.ast.ParserNode


/******************************************************************************
 * DRIFT STATEMENT STRUCTURES
 *
 * Interface for all statements in an AST.
 ******************************************************************************/



/**
 * This interface represents all statement
 * structures.
 */
abstract class ParserStatement : ParserNode()