/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess
import drift.DriftVersion
import drift.runtime.DriftRuntime
import project.DriftProjectLoadingException
import project.ProjectConfig
import project.loadConfig
import kotlin.run

/******************************************************************************
 * DRIFT RUNNER
 *
 * This runner file permits evaluating and executing a Drift file.
 ******************************************************************************/



class Run : CliktCommand(name = "run") {
    private val path: String? by option(
        "-p",
        "--path",
        help = "Project root directory")

    override fun run() {
        val t = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)

        val projectDir =
            if (path != null) File(path!!)
            else File(System.getProperty("user.dir"))

        var config: ProjectConfig

        try {
            config = loadConfig(projectDir)
        } catch (e: DriftProjectLoadingException) {
            cliError(e.message, t)
        }

        val entryPath = Paths
            .get("$projectDir/${config.structure.root}/${config.structure.entry}.drift")
            .normalize()
            .toAbsolutePath()
            .toString()

        val entryFile = File(entryPath)

        if (!entryFile.exists()) {
            cliError("Entry file not found: $entryPath", t)
        }

        t.run {
            println(bold("-- Drift CommandLine Feature — ${DriftVersion.fullVersion} --"))
            println()

            println(bold(
                (driftBlue)("Running ")
                + (rgb("#FFF") on driftBlue)(" Drift ")
                + (driftBlue)(" project ${config.name}")
            ))

            println(bold("Entry: $entryPath\n"))
        }

        val source = entryFile.readText()
        DriftRuntime.run(source, config, projectDir)

        t.run {
            println()
            println(bold(
                (white on black)(" — End of Program — ")
            ))
        }
    }

    override fun help(context: Context): String {
        return "Run a Drift project"
    }
}


val driftBlue = rgb(0.1647058824, 0.5137254902, 1)


class Drift : CliktCommand() {
    override fun run() = Unit
}


internal fun cliError(message: String, t: Terminal): Nothing {
    t.run {
        println(bold(
            (red)("[ERROR] $message")
        ))
    }

    exitProcess(1)
}


fun main(args: Array<String>) = Drift()
    .subcommands(Run())
    .main(args)
