/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.project

import java.io.BufferedReader
import java.io.File


/******************************************************************************
 * DRIFT PROJECT VERSION HELPER
 ******************************************************************************/



/**
 * Drift Distribution Current Version
 */
val Project.version: String
    get() = ((object {}.javaClass
            .getResource("/version.txt")
            ?.readText()
            ?.trim()
            ?: "0.0")
            + "."
            + runGitCommand("rev-parse", "--short", "HEAD").uppercase())



/**
 * Runs a git command in the current directory.
 */
fun runGitCommand(vararg args: String): String {
    val process = ProcessBuilder("git", *args)
        .directory(File(".")) // répertoire de ton projet
        .redirectErrorStream(true)
        .start()

    val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
    process.waitFor()

    return result.trim()
}