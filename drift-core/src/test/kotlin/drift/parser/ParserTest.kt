package drift.parser

import drift.lexer.lex
import drift.parser.exceptions.DPExpectedNewlineBetweenTopLevelStatementsException
import drift.parser.exceptions.DPMissingExpectedTokenException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ParserTest {

    private fun parse(code: String) {
        Parser(lex(code)).parse()
    }

    @Test
    fun `Two top-level statements without newline`() {
        assertThrows<DPExpectedNewlineBetweenTopLevelStatementsException> {
            parse("let x = 1 let y = 2")
        }
    }

    @Test
    fun `Two top-level statements with newline`() {
        assertDoesNotThrow {
            parse("let x = 1\nlet y = 2")
        }
    }

    @Test
    fun `Two block statements without newline`() {
        assertThrows<DPMissingExpectedTokenException> {
            parse("{\n    let x = 1 let y = 2\n}")
        }
    }

    @Test
    fun `Two block statements with newline`() {
        assertDoesNotThrow {
            parse("{\n    let x = 1\n    let y = 2\n}")
        }
    }

    @Test
    fun `Statement with newline inside argument list`() {
        assertDoesNotThrow {
            parse("foo(\n    1\n)")
        }
    }
}