/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser

import drift.exceptions.DriftRuntimeException
import drift.runtime.DriftRuntime
import drift.utils.evalAndGetEnv
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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

    private fun mainCode(source: String) : String {
        val mainFile = File(srcDir, "main.drift")
        mainFile.writeText(source)

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

        return outputStream
            .toString()
            .trim()
    }

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
    fun `Global import without alias`() {
        assertDoesNotThrow {
            val output = mainCode("""
                import hola
                
                print(hola.greeting)
            """.trimIndent())

            assertTrue { output.contains("Hello") }
        }
    }

    @Test
    fun `Global import with alias`() {
        assertDoesNotThrow {
            val output = mainCode("""
                import hola as h
                
                print(h.greeting)
            """.trimIndent())

            assertTrue { output.contains("Hello") }
        }
    }

    @Test
    fun `Global import with alias called with source name must throw`() {
        assertThrows<DriftRuntimeException> {
            mainCode("""
                import hola as h
                
                print(hola.greeting)
            """.trimIndent())
        }
    }

    @Test
    fun `Composed import`() {
        assertDoesNotThrow {
            val output = mainCode("""
                import hola { greeting }
                
                print(greeting)
            """.trimIndent())

            assertTrue { output.contains("Hello") }
        }
    }

    @Test
    fun `Composed import called with module name must throw`() {
        assertThrows<DriftRuntimeException> {
            mainCode("""
                import hola { greeting }
                
                print(hola.greeting)
            """.trimIndent())
        }
    }

    @Test
    fun `Composed import with wildcard only`() {
        assertDoesNotThrow {
            val output = mainCode("""
                import hola { * }
                
                print(greeting)
            """.trimIndent())

            assertTrue { output.contains("Hello") }
        }
    }

    @Test
    fun `Composed import with wildcard and member renaming`() {
        assertDoesNotThrow {
            val output = mainCode("""
                import hola { *, greeting as g }
                
                print(g)
            """.trimIndent())

            assertTrue { output.contains("Hello") }
        }
    }

    @Test
    fun `Composed import with wildcard and member renaming called with source name must throw`() {
        assertThrows<DriftRuntimeException> {
            mainCode("""
                import hola { *, greeting as g }
                
                print(greeting)
            """.trimIndent())
        }
    }
}