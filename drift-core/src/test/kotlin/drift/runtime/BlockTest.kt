/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime

import drift.utils.evalProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class BlockTest {

    @Test
    fun `Shadowing in inner scope is allowed`() {
        assertDoesNotThrow {
            evalProgram("""
                let a = 1
                {
                    let a = 2
                }
            """.trimIndent())
        }
    }
}