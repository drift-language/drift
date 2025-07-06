package drift.helper

import drift.ast.DrExpr
import drift.ast.eval
import drift.exceptions.DriftRuntimeException
import drift.runtime.DrBool
import drift.runtime.DrEnv

fun evalCondition(condition: DrExpr, env: DrEnv) : Boolean {
    val conditionValue = condition.eval(env)

    if (conditionValue !is DrBool) {
        throw DriftRuntimeException("Condition must be boolean")
    }

    return conditionValue.value
}