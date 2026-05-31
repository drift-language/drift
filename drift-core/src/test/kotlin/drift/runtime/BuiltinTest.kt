/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime

import drift.runtime.exceptions.DRUnknownClassMemberException
import drift.utils.evalProgram
import drift.utils.evalWithOutput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BuiltinTest {

    @Test
    fun `Native method on primitive instance`() {
        assertDoesNotThrow {
            val result = evalWithOutput("""
                test("hello".length())
            """.trimIndent())

            assertEquals(5, result.toInt())
        }
    }

    @Test
    fun `Unexisting native method access on primitive instance must throw`() {
        assertThrows<DRUnknownClassMemberException> {
            evalProgram("""
                "hello".x
            """.trimIndent())
        }
    }
}