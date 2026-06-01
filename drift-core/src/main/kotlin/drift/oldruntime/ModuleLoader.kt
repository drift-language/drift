/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime

import drift.ast.statements.Import
import drift.lexer.lex
import drift.parser.Parser
import drift.oldruntime.evaluators.eval
import drift.oldruntime.exceptions.DMLAlreadyImportedModuleException
import drift.oldruntime.exceptions.DMLNotFoundInModuleException
import drift.oldruntime.exceptions.DMLUnexistingModuleException
import drift.oldruntime.values.imports.ParserModule
import drift.oldruntime.values.oop.ParserClass
import project.ProjectConfig
import java.io.File


/******************************************************************************
 * DRIFT RUNTIME MODULE LOADER
 *
 * Runtime Module Loader class.
 ******************************************************************************/



/**
 * The goal of the module loader is to retrieve each import source and
 * do the import itself.
 *
 * Two missions:
 * - Retrieve an import's source (from namespace)
 * - Execute the import by declaring in the provided environment all
 * structures and members of the import's source
 */
class ModuleLoader(
    private val config: ProjectConfig,
    private val projectRootFile: File,
    private val env: DrEnv) {


    private val imported = mutableSetOf<String>()



    /**
     * Import a module using its AST node.
     *
     * A module is searchable using its namespace from the statement:
     * ```drift
     * import drift.module
     * ```
     *
     * A namespace is a dot-represented relative path to the module's source
     * file.
     *
     * @param import Import AST node
     * @throws DMLAlreadyImportedModuleException
     * @throws DMLUnexistingModuleException
     * @throws DMLNotFoundInModuleException
     */
    fun importModule(import: Import) {
        if (imported.contains(import.namespace))
            throw DMLAlreadyImportedModuleException(moduleNamespace = import.namespace)

        val path = File(
            projectRootFile,
            "${config.structure.root}/${import.steps.joinToString("/")}.drift")

        if (!path.exists())
            throw DMLUnexistingModuleException(moduleNamespace = import.namespace)

        val source = path.readText()
        val tokens = lex(source)
        val ast = Parser(tokens).parse()

        val moduleEnv = DrEnv()

//        SymbolCollector(moduleEnv).collect(ast)

        for (stmt in ast) {
            stmt.eval(moduleEnv)
        }

        when {
            import.alias != null -> env.define(
                import.alias,
                ParserModule(
                    import.namespace,
                    import.alias,
                    moduleEnv.export()))

            import.parts != null -> {
                val recorded = mutableSetOf<String>()

                for (part in import.parts) {
                    val partName = part.alias ?: part.source

                    recorded += part.source

                    val sym = moduleEnv.resolve(part.source)
                        ?: moduleEnv.resolveClass(part.source)
                        ?: throw DMLNotFoundInModuleException(
                            element = part.source,
                            moduleNamespace = import.namespace)

                    env.define(partName, sym)
                }

                if (import.wildcard) {
                    for ((k, v) in moduleEnv.export().filter { it.key !in recorded }) {
                        when (v) {
                            is ParserClass  -> env.defineClass(k, v)
                            else        -> env.define(k, v)
                        }
                    }
                }
            }

            else -> env.define(
                import.steps.last(),
                ParserModule(
                    import.namespace,
                    import.namespace,
                    moduleEnv.export()))
        }

        imported.add(import.namespace)
    }
}