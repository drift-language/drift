/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli.bootstraps

import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.italic
import drift.analysis.symbols.SymbolCollector
import drift.analysis.symbols.SymbolTable
import drift.ast.statements.ParserStatement
import drift.bootstrap.Bootstrap
import drift.bootstrap.CompilationMemory
import drift.hir.HIRStatement
import sugar.hasDriftExtension
import sugar.removeDriftExtension
import java.io.File


class RunnerBootstrap(
    sourceRoot: File,
    namespace: String,
    val source: String)
    : Bootstrap(sourceRoot, namespace) {

    val hir: MutableList<HIRStatement> = mutableListOf()


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

                val childBootstrap = RunnerBootstrap(
                    sourceRoot, currentNamespace, source)

                childBootstrap.bootCollectionPass()

                return@map childBootstrap.symbolTable
            }
            .toList()

        val tokens = bootLexer(source)
        ast = bootParser(tokens)

        bootValidation()

        val collection = bootSymbolCollection()

        symbolTable += childSymbolTables

        return collection
    }

    override fun bootCompilationPass(
        collection: SymbolCollector.CollectionResult) {

        val inference = bootTypeInference(collection.resolutions)

        bootCheck(collection.resolutions, inference)

        val analysis = AnalysisResult(
            collection = collection,
            inference = inference)

        hir.addAll(bootHIRConverter(analysis))
    }
}