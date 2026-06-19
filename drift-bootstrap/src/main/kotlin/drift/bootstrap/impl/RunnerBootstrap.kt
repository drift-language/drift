/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.bootstrap.impl

import drift.analysis.symbols.SymbolCollector
import drift.analysis.symbols.SymbolTable
import drift.bootstrap.Bootstrap
import drift.bootstrap.CompilationMemory
import drift.hir.HIRStatement
import language.LangInfo
import language.Namespace
import sugar.hasDriftExtension
import sugar.removeDriftExtension
import java.io.File


class RunnerBootstrap(
    sourceRoot: File,
    namespace: Namespace,
    val source: String)
    : Bootstrap(sourceRoot, namespace) {

    val hir: MutableList<HIRStatement> = mutableListOf()

    private lateinit var collectionResult: SymbolCollector.CollectionResult
    private val childrenBootstraps = mutableSetOf<RunnerBootstrap>()


    override fun boot() {
        CompilationMemory
            .imported
            .clear()

        val collection = bootCollectionPass()
        bootCompilationPass(collection)

        childrenBootstraps.forEach {
            it.symbolTable += symbolTable
            it.bootCompilationPass(it.collectionResult)
        }
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
                    .replace(File.separator, LangInfo.NAMESPACE_SEPARATOR)
                val isDriftFile = currentFileRelativePath
                    .hasDriftExtension()
                val currentNamespace = Namespace(currentFileRelativePath
                    .removeDriftExtension())
                val isAlreadyImported = CompilationMemory
                    .imported
                    .contains(currentNamespace)

                return@filter it.isFile &&
                        isDriftFile &&
                        !isAlreadyImported &&
                        currentNamespace != namespace
            }
            .map { element ->
                val currentNamespace = Namespace(element
                    .toRelativeString(sourceRoot)
                    .replace(File.separator, LangInfo.NAMESPACE_SEPARATOR)
                    .removeDriftExtension())

                val source = element.readText()

                val childBootstrap = RunnerBootstrap(
                    sourceRoot, currentNamespace, source)

                childBootstrap.bootHandleFile()

                childrenBootstraps.add(childBootstrap)

                return@map childBootstrap.symbolTable
            }
            .toList()

        symbolTable += childSymbolTables

        return bootHandleFile()
    }

    private fun bootHandleFile() : SymbolCollector.CollectionResult {
        val tokens = bootLexer(source)
        ast = bootParser(tokens)

        bootValidation()

        val collection = bootSymbolCollection()

        collectionResult = collection

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