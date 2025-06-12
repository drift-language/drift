package drift

import drift.ast.eval
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.DrEnv
import drift.runtime.DrFunction
import drift.runtime.DrNull

fun main() {
//    val env = DrEnv()
//    env.define("print", DrFunction { args ->
//        println(args.joinToString(" ") { it.asString() })
//        DrNull
//    })
//
//    val parser = Parser(lex("print(true)"))
//    val stmt = parser.parse()
//
//    stmt.eval(env)
}