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
import java.io.File


/**
 * A bootstrap is a build orchestrator.
 * It handles each step and data structure from
 * the source code to the compilation end.
 *
 * @author Jonathan (GitHub: belicfr)
 */
abstract class Bootstrap(
    protected val sourceRoot: File,
    protected val namespace: String) {

    protected lateinit var ast: List<ParserStatement>
    protected val symbolTable = SymbolTable()


    /**
     * This method is the entry point of the bootstrap
     * class.
     *
     * It handles both main build passes:
     * - Collection
     * - Compilation
     */
    abstract fun boot()

    /**
     * Collection is the first pass of the build
     * process.
     *
     * It contains many steps:
     * - Lexing
     * - Parsing
     * - Structure validation
     * - Symbol collection
     */
    protected abstract fun bootCollectionPass(): SymbolCollector.CollectionResult

    /**
     * Compilation is the second and last pass of
     * the build process.
     *
     * It contains many steps:
     * - Type handling (inference and checking)
     * - High Intermediate Representation (HIR)
     * - Backend
     */
    protected abstract fun bootCompilationPass(
        collection: SymbolCollector.CollectionResult)


    protected fun bootLexer(source: String) : List<Token> {
        return lex(source)
    }


    protected fun bootParser(tokens: List<Token>) : List<ParserStatement> {
        return Parser(tokens)
            .parse()
    }


    protected fun bootValidation() {
        ast.forEach { node ->
            when (node) {
                is Class -> ClassValidator(node).validate()
            }
        }
    }

    protected fun bootSymbolCollection() : SymbolCollector.CollectionResult {
        return SymbolCollector(namespace, symbolTable, ast)
            .collect()
    }

    protected fun bootTypeInference(
        refResolutions: Map<Int, Int>) : TypeInference.TypeInferenceResult {

        return TypeInference(ast, symbolTable, refResolutions)
            .infer()
    }

    protected fun bootCheck(
        refResolutions: Map<Int, Int>,
        resolutions: TypeInference.TypeInferenceResult) {

        val typeChecker = TypeChecker(
            namespace = namespace,
            ast = ast,
            symbolTable = symbolTable,
            refResolutions = refResolutions,
            resolutions = resolutions)

        typeChecker.check()
    }


    protected fun bootHIRConverter(
        analysis: AnalysisResult)
        : List<HIRStatement> {

        val converter = HIRConverter(
            namespace = namespace,
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