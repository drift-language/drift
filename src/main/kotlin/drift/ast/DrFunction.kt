package drift.ast

import drift.runtime.AnyType
import drift.runtime.DrType

data class Function(
    val name: String,
    val parameters: List<FunctionParameter>,
    val body: List<DrStmt>,
    val returnType: DrType = AnyType) : DrStmt

data class FunctionParameter(
    val name: String,
    val isPositional: Boolean = false,
    val type: DrType = AnyType)