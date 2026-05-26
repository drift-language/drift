/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.cli.bootstraps

import drift.analysis.inference.TypeInference
import drift.analysis.symbols.SymbolCollector
import drift.ast.statements.ParserStatement
import drift.bootstrap.Bootstrap
import drift.hir.HIRStatement


class RunnerBootstrap(val source: String) : Bootstrap() {

    val hir: MutableList<HIRStatement> = mutableListOf()


    override fun boot() {
        val tokens = bootLexer(source)
        val ast = bootParser(tokens)
        val analysis = bootAnalysis(ast)

        hir += bootHIRConverter(ast, analysis)
    }

    override fun bootAnalysis(ast: List<ParserStatement>): AnalysisResult {
        bootValidation(ast)

        val collection = bootSymbolCollection(ast)
        val inference = bootTypeInference(ast, collection.resolutions)

        bootCheck(ast, collection.resolutions, inference)

        return AnalysisResult(inference, collection)
    }
}