package drift.utils

import drift.ast.eval
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.DrEnv
import drift.runtime.DrNull
import drift.runtime.DrValue

fun evalProgram(source: String) : DrValue {
    val tokens = lex(source)
    val statements = Parser(tokens).parse()
    val env = DrEnv()

    var result: DrValue = DrNull

    for (statement in statements) {
        result = statement.eval(env)
    }

    return result
}