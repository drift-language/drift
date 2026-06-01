/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime

import drift.parser.exceptions.DPNumericSizeOverflowException
import drift.oldruntime.exceptions.DRCannotNegateUnsignedException
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.primaries.ParserInt64
import drift.oldruntime.values.primaries.ParserUInt
import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PrimaryTest {

    @Test
    fun `Define valid 32-Int variable without explicit typing must type as Int`() {
        assertDoesNotThrow {
            val result = evalProgram("""
                let a = 100
            """.trimIndent())

            assertEquals(result, ParserInt(100))
        }
    }

    @Test
    fun `Define valid 64-Int variable without explicit typing must type as Int64`() {
        assertDoesNotThrow {
            val result = evalProgram("""
                let a = 10000000000000
            """.trimIndent())

            assertEquals(result, ParserInt64(10000000000000))
        }
    }

    @Test
    fun `Define invalid numeric variable without explicit typing must throw`() {
        assertThrows<DPNumericSizeOverflowException> {
            evalProgram("""
                let a = 1000000000000000000000000000000000000
            """.trimIndent())
        }
    }

    @Test
    fun `Define negative numeric as unsigned integer must throw`() {
        assertThrows<DRCannotNegateUnsignedException> {
            evalProgram("""
                let a: UInt = -2
            """.trimIndent())
        }
    }

    @Test
    fun `Define valid UInt variable with explicit typing must type as UInt`() {
        assertDoesNotThrow {
            val result = evalProgram("""
                let a: UInt = 2
            """.trimIndent())

            assertEquals(result, ParserUInt(2u))
        }
    }
}