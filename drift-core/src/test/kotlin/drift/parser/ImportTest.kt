/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser

import drift.runtime.DriftRuntime
import drift.utils.evalAndGetEnv
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import project.ProjectConfig
import project.loadConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.assertTrue

class ImportTest {

    private var tempDir: File? = null
    private var srcDir: File? = null
    private var projectConfig: ProjectConfig? = null

    @BeforeEach
    fun setUp() {
        tempDir = Files
            .createTempDirectory("drift-test")
            .toFile()

        val configFile = File(tempDir, "drift.json")
        configFile.writeText("""
            {
                "name": "TestProject",
                "structure": {
                    "root": "src", 
                    "entry": "main"
                }
            }
        """.trimIndent())

        srcDir = File(tempDir, "src")
        srcDir?.mkdirs()
        File(srcDir, "hola.drift")
            .writeText("""
                let greeting = "Hello"
            """.trimIndent())

        this.projectConfig = loadConfig(tempDir!!)
    }

    @Test
    fun `Global Import without alias`() {
        val mainFile = File(srcDir, "main.drift")
        mainFile.writeText("""
            import hola
            
            print(hola.greeting)
        """.trimIndent())

        val outputStream = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            DriftRuntime.run(
                mainFile.readText(),
                projectConfig!!,
                tempDir!!)
        } finally {
            System.setOut(oldOut)
        }

        assertDoesNotThrow {
            val output = outputStream
                .toString()
                .trim()

            assertTrue { output.contains("Hello") }
        }
    }
}