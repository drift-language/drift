/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.bootstrap

import drift.analysis.checkers.TypeChecker
import drift.analysis.inference.TypeInference
import drift.analysis.semantic.classes.ClassValidator
import drift.analysis.symbols.SymbolCollector
import drift.analysis.symbols.SymbolTable
import drift.ast.statements.Class
import drift.ast.statements.ParserStatement
import drift.hir.HIRConverter
import drift.hir.HIRStatement
import drift.lexer.Token
import drift.lexer.lex
import drift.parser.Parser


abstract class Bootstrap {

    protected val symbolTable = SymbolTable()


    abstract fun boot()

    protected abstract fun bootAnalysis(ast: List<ParserStatement>) : AnalysisResult


    protected fun bootLexer(source: String) : List<Token> {
        return lex(source)
    }


    protected fun bootParser(tokens: List<Token>) : List<ParserStatement> {
        return Parser(tokens)
            .parse()
    }


    protected fun bootValidation(ast: List<ParserStatement>) {
        ast.forEach { node ->
            when (node) {
                is Class -> ClassValidator(node).validate()
            }
        }
    }

    protected fun bootSymbolCollection(ast: List<ParserStatement>) : SymbolCollector.CollectionResult {
        return SymbolCollector(symbolTable, ast)
            .collect()
    }

    protected fun bootTypeInference(
        ast: List<ParserStatement>,
        refResolutions: Map<Int, Int>) : TypeInference.TypeInferenceResult {

        return TypeInference(ast, symbolTable, refResolutions)
            .infer()
    }

    protected fun bootCheck(
        ast: List<ParserStatement>,
        refResolutions: Map<Int, Int>,
        resolutions: TypeInference.TypeInferenceResult) {

        val typeChecker = TypeChecker(
            ast = ast,
            symbolTable = symbolTable,
            refResolutions = refResolutions,
            resolutions = resolutions)

        typeChecker.check()
    }


    protected fun bootHIRConverter(
        ast: List<ParserStatement>,
        analysis: AnalysisResult)
        : List<HIRStatement> {

        val converter = HIRConverter(
            ast = ast,
            symbolTable = symbolTable,
            typeResolution = analysis.inference.typeResolutions,
            lambdaClosures = analysis.collection.lambdaClosures)

        return converter.convert()
    }


    protected data class AnalysisResult(
        val inference: TypeInference.TypeInferenceResult,
        val collection: SymbolCollector.CollectionResult)
}