package drift.ast

data class Function(
    val name: String,
    val parameters: List<FunctionParameter>,
    val body: List<DrStmt>) : DrStmt

data class FunctionParameter(
    val name: String,
    val isPositional: Boolean = false)