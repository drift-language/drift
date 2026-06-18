/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.bootstrap.impl

import drift.analysis.inference.TypeInference
import drift.analysis.symbols.SymbolCollector
import drift.analysis.symbols.SymbolTable
import drift.bootstrap.Bootstrap
import drift.bootstrap.CompilationMemory
import drift.hir.HIRStatement
import drift.lexer.Token
import language.LangInfo
import sugar.hasDriftExtension
import sugar.removeDriftExtension
import java.io.File


class RunnerTestBootstrap(
    sourceRoot: File,
    namespace: String,
    val source: String)
    : Bootstrap(sourceRoot, namespace) {

    private val childrenBootstraps = mutableSetOf<RunnerTestBootstrap>()

    val parsedAst get() = ast

    lateinit var tokens: List<Token>
        private set

    lateinit var collection: SymbolCollector.CollectionResult
        private set

    lateinit var inference: TypeInference.TypeInferenceResult
        private set

    lateinit var hir: List<HIRStatement>
        private set


    override fun boot() {
        CompilationMemory
            .imported
            .clear()

        val collection = bootCollectionPass()
        bootCompilationPass(collection)

        childrenBootstraps.forEach {
            it.symbolTable += symbolTable
            it.bootCompilationPass(it.collection)
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
                    .replace(File.separator, LangInfo.NAMESPACE_SEPARATOR)
                    .removeDriftExtension()

                val source = element.readText()

                val childBootstrap = RunnerTestBootstrap(
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
        tokens = bootLexer(source)
        ast = bootParser(tokens)

        bootValidation()

        collection = bootSymbolCollection()

        return collection
    }

    override fun bootCompilationPass(
        collection: SymbolCollector.CollectionResult) {

        inference = bootTypeInference(collection.resolutions)

        bootCheck(collection.resolutions, inference)

        val analysis = AnalysisResult(
            collection = collection,
            inference = inference)

        hir = bootHIRConverter(analysis)

        /*
         *      BACKEND IMPLEMENTATION: JVM
         *
         * todo
         */
    }
}
