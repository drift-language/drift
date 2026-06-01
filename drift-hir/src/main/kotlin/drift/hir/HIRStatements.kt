/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import drift.ast.metadata.Annotation
import drift.hir.metadata.HIRAnnotation

/**
 * Function declaration in HIR.
 */
data class HIRFunction(
    override val hirId: Int,
    override val annotations: MutableList<HIRAnnotation>,
    val name: String,
    val parameters: List<HIRParameter>,
    val returnType: HIRType,
    val body: List<HIRStatement>,
    val isStatic: Boolean
) : HIRStatement, HIRAnnotatable

/**
 * Function parameter.
 */
data class HIRParameter(
    val name: String,
    val type: HIRType,
    val defaultValue: HIRExpression? = null
)

/**
 * Variable declaration (let/var).
 */
data class HIRVariable(
    override val hirId: Int,
    override val annotations: MutableList<HIRAnnotation>,
    val name: String,
    val type: HIRType,
    val initialValue: HIRExpression? = null,
    val isMutable: Boolean
) : HIRStatement, HIRAnnotatable

/**
 * Class declaration.
 */
data class HIRClass(
    override val hirId: Int,
    override val annotations: MutableList<HIRAnnotation>,
    val name: String,
    val fields: List<HIRField>,
    val methods: List<HIRFunction>,
    val staticFields: List<HIRField>,
    val staticMethods: List<HIRFunction>
) : HIRStatement, HIRAnnotatable

/**
 * Class field definition.
 */
data class HIRField(
    override val hirId: Int,
    val name: String,
    override val annotations: MutableList<HIRAnnotation>,
    val type: HIRType,
    val isStatic: Boolean,
) : HIRStatement, HIRAnnotatable

/**
 * Code block (scoped statements).
 */
data class HIRBlock(
    override val hirId: Int,
    val statements: List<HIRStatement>
) : HIRStatement

/**
 * Return statement.
 */
data class HIRReturn(
    override val hirId: Int,
    val value: HIRExpression? = null
) : HIRStatement

/**
 * Expression as a statement.
 */
data class HIRExpressionStmt(
    override val hirId: Int,
    val expression: HIRExpression
) : HIRStatement

/**
 * Module import statement.
 */
data class HIRImport(
    override val hirId: Int,
    val namespace: String,
    val steps: List<String>,
    val alias: String? = null,
    val parts: List<HIRImportPart>? = null,
    val wildcard: Boolean = false
) : HIRStatement

/**
 * Part of an import statement (for selective imports).
 */
data class HIRImportPart(
    val source: String,
    val alias: String? = null
)
