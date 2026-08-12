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
import language.Namespace


sealed interface HIRStatementCallable : HIRCallable

/**
 * Function declaration in HIR.
 */
data class HIRFunction(
    override val hirId: HirId,
    override val annotations: MutableList<HIRAnnotation>,
    override val parameters: List<HIRParameter>,
    override val returnType: HIRType,
    override val body: List<HIRStatement>,
    val name: String,
    val capturedVariables: List<HIRCapturedVariable>) : HIRStatement, HIRAnnotatable, HIRStatementCallable

/**
 * Class method declaration in HIR.
 */
data class HIRMethod(
    override val hirId: HirId,
    override val annotations: MutableList<HIRAnnotation>,
    override val parameters: List<HIRParameter>,
    override val returnType: HIRType,
    override val body: List<HIRStatement>,
    val name: String,
    val isStatic: Boolean) : HIRStatement, HIRAnnotatable, HIRStatementCallable

/**
 * Hook declaration in HIR.
 */
data class HIRHook(
    override val hirId: HirId,
    val name: String,
    val parameters: List<HIRParameter>,
    val returnType: HIRType,
    val body: List<HIRStatement>) : HIRStatement

/**
 * Function parameter.
 */
data class HIRParameter(
    override val hirId: HirId,
    val name: String,
    val type: HIRType,
    val defaultValue: HIRExpression? = null) : HIRNode

/**
 * Variable declaration (let/var).
 */
data class HIRVariable(
    override val hirId: HirId,
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
    override val hirId: HirId,
    override val annotations: MutableList<HIRAnnotation>,
    val name: String,
    val fields: List<HIRField>,
    val methods: List<HIRMethod>,
    val hooks: List<HIRHook>,
    val staticFields: List<HIRField>,
    val staticMethods: List<HIRMethod>
) : HIRStatement, HIRAnnotatable

/**
 * Class field definition.
 */
data class HIRField(
    override val hirId: HirId,
    val name: String,
    override val annotations: MutableList<HIRAnnotation>,
    val type: HIRType,
    val isStatic: Boolean,
) : HIRStatement, HIRAnnotatable

/**
 * Code block (scoped statements).
 */
data class HIRBlock(
    override val hirId: HirId,
    val statements: List<HIRStatement>
) : HIRStatement

/**
 * Return statement.
 */
data class HIRReturn(
    override val hirId: HirId,
    val value: HIRExpression? = null
) : HIRStatement

/**
 * Expression as a statement.
 */
data class HIRExpressionStmt(
    override val hirId: HirId,
    val expression: HIRExpression
) : HIRStatement

/**
 * Module import statement.
 */
data class HIRImport(
    override val hirId: HirId,
    val namespace: Namespace,
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
