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
import com.github.ajalt.mordant.rendering.TextStyles.italic
import com.github.ajalt.mordant.terminal.Terminal
import drift.DriftVersion
import drift.bootstrap.impl.RunnerTestBootstrap
import language.LangInfo.NAMESPACE_SEPARATOR
import language.Namespace
import project.loadConfig
import sugar.removeDriftExtension
import java.io.File

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

    val outputRoot = projectRoot
        .absoluteFile
        .resolve(config.structure.output)

    if (!sourceRoot.isDirectory)
        error("Source root must be a directory")

    val namespace = Namespace(sourceRootPath
        .relativize(file.absoluteFile.toPath())
        .toString()
        .replace(File.separator, NAMESPACE_SEPARATOR)
        .removeDriftExtension())

    val bootstrap = RunnerTestBootstrap(
        sourceRoot = sourceRoot,
        namespace = namespace,
        source = source,
        output = outputRoot)

    bootstrap.boot()

    t.run {
        println(bold(yellow("[TOKENS]\t\t")) +
                italic(bootstrap.tokens.toString()))
        println(bold(red("[AST]\t\t\t")) +
                italic(bootstrap.parsedAst.toString()))
        println(bold(magenta("[SYM COLLECTION]\t")) +
                italic(bootstrap.collection.toString()))
        println(bold(brightRed("[TYPE INFERENCE]\t")) +
                italic(bootstrap.inference.toString()))
        println(bold(brightGreen("[TYPE CHECKING]\t\t")) +
                italic("Passed (none exception)."))
        println(bold(green("[HIR]\t\t\t")) +
                italic(bootstrap.hir.toString()))
        println("\n——————\n")
    }

    //

    t.run {
        println()
        println(bold(
            (white on black)(" — End of Program — ")))
    }
}