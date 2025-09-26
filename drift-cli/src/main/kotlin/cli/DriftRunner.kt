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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import kotlin.system.exitProcess
import drift.DriftVersion
import drift.runtime.DriftRuntime

/******************************************************************************
 * DRIFT RUNNER
 *
 * This runner file permits evaluating and executing a Drift file.
 ******************************************************************************/


@Serializable
data class ProjectConfig(
    val name: String = "Unnamed Project",
    val structure: ProjectStructure = ProjectStructure(
        "./src",
        "main"))

@Serializable
data class ProjectStructure(
    val root: String,
    val entry: String)


fun loadConfig(dir: File) : ProjectConfig {
    val configFile = File(dir, "drift.json")

    if (!configFile.exists()) {
        cliError("Config file does not exist: ${configFile.absolutePath}")
    }

    val json = configFile.readText()

    return Json.decodeFromString(ProjectConfig.serializer(), json)
}


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

        val config = loadConfig(projectDir)

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
        DriftRuntime.run(source)

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
