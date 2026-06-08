/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import drift.DriftVersion
import drift.cli.bootstraps.RunnerTestBootstrap
import language.LangInfo
import project.loadConfig
import sugar.removeDriftExtension
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun main(args: Array<String>) {
    val t = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)

    t.run {
        println(bold(
            "-- Drift CommandLine Debugger Feature — ${DriftVersion.fullVersion} --"
        ))

        println()
        println(bold(
            (driftBlue)("Running ") +
            (rgb("#FFF") on driftBlue)(" Drift ") +
            (rgb("#FFF") on green)(" Debugger ")
        ))
    }

    if (args.isEmpty()) return

    t.run {
        println(bold("Entry: ${args[0]}"))
        println()
    }

    val file = File(args[0])

    if (!file.exists()) {
        cliError("File not found: ${args[0]}", t)
    }

    val projectRoot = File("examples")

    val source = file.readText()
    val config = loadConfig(projectRoot)

    val sourceRootPath = projectRoot
        .absoluteFile
        .resolve(config.structure.root)
        .toPath()
    val sourceRoot = sourceRootPath.toFile()

    if (!sourceRoot.isDirectory)
        error("Source root must be a directory")

    val namespace = sourceRootPath
        .relativize(Path.of(args[0]))
        .toString()
        .removeDriftExtension()

    RunnerTestBootstrap(
        sourceRoot = sourceRoot,
        namespace = namespace,
        source = source).boot()


    t.run {
        println("\n——————\n")
    }

    //

    t.run {
        println()
        println(bold(
            (white on black)(" — End of Program — ")))
    }
}