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
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.italic
import com.github.ajalt.mordant.terminal.Terminal
import drift.analysis.inference.TypeInference
import drift.analysis.symbols.SymbolCollector
import drift.ast.statements.ParserStatement
import drift.bootstrap.Bootstrap


class RunnerTestBootstrap(
    val source: String) : Bootstrap() {

    private val t = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)


    override fun boot() {
        val tokens = bootLexer(source)
        t.println(
            bold(yellow("[TOKENS]\t\t")) +
            italic(tokens.toString()))


        val ast = bootParser(tokens)
        t.println(
            bold(red("[AST]\t\t\t")) +
            italic(ast.toString()))


        val analysis = bootAnalysis(ast)


        val hir = bootHIRConverter(ast, analysis)
        t.println(
            bold(green("[HIR]\t\t\t")) +
            italic(hir.toString()))
    }

    override fun bootAnalysis(ast: List<ParserStatement>) : AnalysisResult {
        bootValidation(ast)

        val collection = bootSymbolCollection(ast)

        t.println(
            bold(magenta("[SYM COLLECTION]\t")) +
            italic(collection.toString()))

        t.println(
            bold(cyan("[SYM TABLE]\t\t")) +
            italic(symbolTable.toString()))

        val inference = bootTypeInference(ast, collection.resolutions)

        t.println(
            bold(brightRed("[TYPE INFERENCE]\t")) +
            italic(inference.toString()))

        bootCheck(ast, collection.resolutions, inference)

        t.println(
            bold(brightGreen("[TYPE CHECKING]\t\t")) +
            italic("Passed (none exception)."))


        return AnalysisResult(inference, collection)
    }
}