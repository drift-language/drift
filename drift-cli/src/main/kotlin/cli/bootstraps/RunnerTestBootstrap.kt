/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli.bootstraps

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.italic
import com.github.ajalt.mordant.terminal.Terminal
import drift.analysis.symbols.SymbolCollector
import drift.analysis.symbols.SymbolTable
import drift.bootstrap.Bootstrap
import drift.bootstrap.CompilationMemory
import sugar.hasDriftExtension
import sugar.removeDriftExtension
import java.io.File


class RunnerTestBootstrap(
    sourceRoot: File,
    namespace: String,
    val source: String)
    : Bootstrap(sourceRoot, namespace) {

    private val t = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)


    override fun boot() {
        CompilationMemory
            .imported
            .clear()

        val collection = bootCollectionPass()
        bootCompilationPass(collection)
    }

    override fun bootCollectionPass(): SymbolCollector.CollectionResult {
        if (!sourceRoot.isDirectory())
            error("Source root path must target the source directory")


        CompilationMemory
            .imported
            .add(namespace)

        val childSymbolTables: List<SymbolTable> = sourceRoot
            .walkTopDown()
            .filter {
                val currentFileRelativePath = it
                    .toRelativeString(sourceRoot)
                val isDriftFile = currentFileRelativePath
                    .hasDriftExtension()
                val currentNamespace = currentFileRelativePath
                    .removeDriftExtension()
                val isAlreadyImported = CompilationMemory
                    .imported
                    .contains(currentNamespace)

                return@filter it.isFile &&
                    isDriftFile &&
                    !isAlreadyImported &&
                    currentNamespace != namespace
            }
            .map { element ->
                val currentNamespace = element
                    .toRelativeString(sourceRoot)
                    .removeDriftExtension()

                val source = element.readText()

                println("[BOOTSTRAP]\tImporting: $currentNamespace")

                val childBootstrap = RunnerTestBootstrap(
                    sourceRoot, currentNamespace, source)

                childBootstrap.bootCollectionPass()

                return@map childBootstrap.symbolTable
            }
            .toList()

        println();println()


        symbolTable += childSymbolTables


        val tokens = bootLexer(source)
        t.println(
            bold(yellow("[TOKENS]\t\t")) +
                    italic(tokens.toString()))


        ast = bootParser(tokens)
        t.println(
            bold(red("[AST]\t\t\t")) +
                    italic(ast.toString()))

        bootValidation()

        val collection = bootSymbolCollection()

        t.println(
            bold(magenta("[SYM COLLECTION]\t")) +
                    italic(collection.toString()))

        t.println(
            bold(cyan("[SYM TABLE]\t\t")) +
                    italic(symbolTable.toString()))

        println("[BOOTSTRAP]\tSYMTABLE from $namespace = $symbolTable")

        return collection
    }

    override fun bootCompilationPass(
        collection: SymbolCollector.CollectionResult) {

        val inference = bootTypeInference(collection.resolutions)

        t.println(
            bold(brightRed("[TYPE INFERENCE]\t")) +
                    italic(inference.toString()))

        bootCheck(collection.resolutions, inference)

        t.println(
            bold(brightGreen("[TYPE CHECKING]\t\t")) +
                    italic("Passed (none exception)."))

        val analysis = AnalysisResult(
            collection = collection,
            inference = inference)

        val hir = bootHIRConverter(analysis)
        t.println(
            bold(green("[HIR]\t\t\t")) +
                    italic(hir.toString()))
    }
}