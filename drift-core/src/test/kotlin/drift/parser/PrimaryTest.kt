/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser

import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.primaries.DrInt64
import drift.runtime.values.primaries.DrUInt
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

            assertEquals(result, DrInt(100))
        }
    }

    @Test
    fun `Define valid 64-Int variable without explicit typing must type as Int64`() {
        assertDoesNotThrow {
            val result = evalProgram("""
                let a = 10000000000000
            """.trimIndent())

            assertEquals(result, DrInt64(10000000000000))
        }
    }

    @Test
    fun `Define invalid numeric variable without explicit typing must throw`() {
        assertThrows<DriftParserException> {
            evalProgram("""
                let a = 1000000000000000000000000000000000000
            """.trimIndent())
        }
    }

    @Test
    fun `Define negative numeric as unsigned integer must throw`() {
        assertThrows<DriftRuntimeException> {
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

            assertEquals(result, DrUInt(2u))
        }
    }
}