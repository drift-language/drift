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
    private val path: String? by option("-p", "--path", help = "Project root directory")

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

        println(
            Ansi.ansi()
                .fgBrightBlue()
                .bold()
                .a("Running Drift project ${config.name}")
                .reset())

        println(
            Ansi.ansi()
                .bold()
                .a("Entry: $entryPath")
                .reset())

        AnsiConsole.systemUninstall()
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


/*
@Deprecated("Use Drift CLI instead")
fun __main(args: Array<String>) {
    println("-- Drift CommandLine Feature — ${Project.version} --\n")

    if (args.isEmpty()) {
        println("Usage: drift <file.drift>")
        return
    }

    val file = File(args[0])

    if (!file.exists()) {
        println("File not found: ${args[0]}")
        return
    }

    val source = file.readText()

    val env = DrEnv()

    val tokens = lex(source)
    val ast = Parser(tokens).parse()
    println(ast)

    env.run {
        define("print", DrNativeFunction(
            impl = { _, args ->
                println(args.joinToString(" ") { it.second.asString() })
                DrVoid
            },
            paramTypes = listOf(AnyType),
            returnType = NullType
        ))

        defineClass("String", DrClass("String", emptyList(), listOf(
            DrMethod(
                let = Function(
                    name = "length",
                    parameters = emptyList(),
                    returnType = ObjectType("Int"),
                    body = emptyList()
                ),
                closure = env,
                nativeImpl = DrNativeFunction(
                    name = "length",
                    paramTypes = emptyList(),
                    returnType = ObjectType("Int"),
                    impl = { receiver, args ->
                        val instance = receiver as? DrString
                            ?: throw DriftRuntimeException("length() called on non-String")

                        DrInt(instance.value.length)
                    }
                )
            )
        )))

        defineClass("Int", DrClass("Int", emptyList(), emptyList()))
        defineClass("Bool", DrClass("Bool", emptyList(), emptyList()))
        defineClass("Int64", DrClass("Int64", emptyList(), emptyList()))
        defineClass("UInt", DrClass("UInt", emptyList(), emptyList()))
    }

    SymbolCollector(env).collect(ast)
    TypeChecker(env).check(ast)

    for (statement in ast) {
        statement.eval(env)
    }
}

 */