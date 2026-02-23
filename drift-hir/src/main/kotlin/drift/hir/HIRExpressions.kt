/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

// ============================================================================
// OPERATORS
// ============================================================================

enum class BinaryOperator {
    ADD, SUB, MUL, DIV, MOD,
    EQ, NEQ, LT, LTE, GT, GTE,
    AND, OR,
    INCLUSIVE_RANGE, EXCLUSIVE_RANGE
}

enum class UnaryOperator {
    NEGATE, NOT
}

// ============================================================================
// LITERAL & REFERENCE
// ============================================================================

data class HIRLiteral(
    override val hirId: Int,
    override val type: HIRType,
    val value: Any?
) : HIRExpression

data class HIRVariableRef(
    override val hirId: Int,
    override val type: HIRType,
    val name: String,
    val definitionHirId: Int
) : HIRExpression

// ============================================================================
// ARITHMETIC & LOGICAL
// ============================================================================

data class HIRBinaryOp(
    override val hirId: Int,
    override val type: HIRType,
    val operator: BinaryOperator,
    val left: HIRExpression,
    val right: HIRExpression
) : HIRExpression

data class HIRUnaryOp(
    override val hirId: Int,
    override val type: HIRType,
    val operator: UnaryOperator,
    val operand: HIRExpression
) : HIRExpression

// ============================================================================
// FUNCTION & METHOD CALLS
// ============================================================================

data class HIRCall(
    override val hirId: Int,
    override val type: HIRType,
    val callee: HIRExpression,
    val arguments: List<HIRArgument>
) : HIRExpression

data class HIRArgument(
    val name: String?,
    val value: HIRExpression
)

// ============================================================================
// OBJECT ACCESS
// ============================================================================

data class HIRFieldAccess(
    override val hirId: Int,
    override val type: HIRType,
    val receiver: HIRExpression,
    val fieldName: String,
    val fieldOffset: Int,
    val receiverClassName: String
) : HIRExpression

// ============================================================================
// ASSIGNMENT
// ============================================================================

data class HIRAssign(
    override val hirId: Int,
    override val type: HIRType,
    val target: AssignTarget,
    val value: HIRExpression
) : HIRExpression

sealed interface AssignTarget

data class VariableTarget(val name: String) : AssignTarget
data class FieldTarget(
    val receiver: HIRExpression,
    val fieldName: String,
    val fieldOffset: Int
) : AssignTarget

// ============================================================================
// CONTROL FLOW
// ============================================================================

data class HIRConditional(
    override val hirId: Int,
    override val type: HIRType,
    val condition: HIRExpression,
    val thenBranch: HIRBlock,
    val elseBranch: HIRBlock?
) : HIRExpression

data class HIRLoop(
    override val hirId: Int,
    val iteratorVariable: String,
    val iteratorType: HIRType,
    val iterable: HIRExpression,
    val body: HIRBlock
) : HIRExpression {
    override val type: HIRType = HIRPrimitiveType(PrimitiveKind.VOID)
}

// ============================================================================
// LAMBDAS & CLOSURES
// ============================================================================

data class HIRLambda(
    override val hirId: Int,
    override val type: HIRType,
    val parameters: List<HIRLambdaParameter>,
    val capturedVariables: List<HIRCapturedVariable>,
    val body: List<HIRStatement>
) : HIRExpression

data class HIRLambdaParameter(
    val name: String,
    val type: HIRType
)

data class HIRCapturedVariable(
    val name: String,
    val type: HIRType,
    val definitionHirId: Int
)
