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
import java.io.File
import java.nio.file.Paths
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import kotlin.system.exitProcess
import drift.DriftVersion
import drift.runtime.DriftRuntime
import project.DriftProjectLoadingException
import project.ProjectConfig
import project.loadConfig

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
        AnsiConsole.systemInstall()

        val projectDir =
            if (path != null) File(path!!)
            else File(System.getProperty("user.dir"))

        var config: ProjectConfig

        try {
            config = loadConfig(projectDir)
        } catch (e: DriftProjectLoadingException) {
            cliError(e.message)
        }

        val entryPath = Paths
            .get("$projectDir/${config.structure.root}/${config.structure.entry}.drift")
            .normalize()
            .toAbsolutePath()
            .toString()

        val entryFile = File(entryPath)

        if (!entryFile.exists()) {
            cliError("Entry file not found: $entryPath")
        }

        println(
            Ansi.ansi()
                .bgBrightDefault()
                .bold()
                .a("-- Drift CommandLine Feature — ${DriftVersion.fullVersion} --\n")
                .reset())

        val driftBlue: Triple<Int, Int, Int> = Triple(42, 131, 255)

        println(
            Ansi.ansi()
                .fgRgb(driftBlue.first, driftBlue.second, driftBlue.third)
                .bold()
                .a("Running ")
                .reset()
                .bgRgb(driftBlue.first, driftBlue.second, driftBlue.third)
                .fgRgb(255, 255, 255)
                .bold()
                .a(" Drift ")
                .reset()
                .fgRgb(driftBlue.first, driftBlue.second, driftBlue.third)
                .bold()
                .a(" project ${config.name}")
                .reset())

        println(
            Ansi.ansi()
                .bold()
                .a("Entry: $entryPath\n")
                .reset())

        AnsiConsole.systemUninstall()

        val source = entryFile.readText()
        DriftRuntime.run(source, config, projectDir)

        println()
        println(
            Ansi.ansi()
                .bgRgb(0)
                .fgRgb(255, 255, 255)
                .bold()
                .a(" — End of Program — ")
                .reset())
    }

    override fun help(context: Context): String {
        return "Run a Drift project"
    }
}


class Drift : CliktCommand() {
    override fun run() = Unit
}


internal fun cliError(message: String): Nothing {
    val styled = Ansi.ansi()
        .fgRed()
        .bold()
        .a("[ERROR] $message")
        .reset()

    println(styled)
    exitProcess(1)
}


fun main(args: Array<String>) = Drift()
    .subcommands(Run())
    .main(args)
