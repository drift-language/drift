package drift.parser

import drift.ast.statements.DrStmt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class DriftIfTest {

    private fun parse(code: String): List<DrStmt> {
        return Parser(lex(code)).parse()
    }

    @Test
    fun `Chained drift-styled IF-ELSE with braces`() {
        assertDoesNotThrow {
            parse("""
                x == 0 ? {
                    return 1
                } : x > 2 ? {
                    return 3
                } : {
                    return 4
                }
            """.trimIndent())
        }
    }

    @Test
    fun `Chained inline drift-styled IF-ELSE`() {
        assertDoesNotThrow {
            parse("""
                x == 1 ? test() : x > 2 ? test() : test()
            """.trimIndent())
        }
    }
}