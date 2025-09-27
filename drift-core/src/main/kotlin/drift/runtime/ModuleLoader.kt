/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime

import drift.ast.statements.Import
import drift.exceptions.DriftRuntimeException
import drift.parser.Parser
import drift.parser.lex
import drift.runtime.evaluators.eval
import drift.runtime.values.imports.DrModule
import drift.runtime.values.oop.DrClass
import drift.runtime.values.variables.DrVariable
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

    fun importModule(import: Import) {
        if (imported.contains(import.namespace))
            throw DriftRuntimeException("Module '${import.namespace}' already imported")

        val path = File(
            projectRootFile,
            "${config.structure.root}/${import.steps.joinToString("/")}.drift")

        if (!path.exists())
            throw DriftRuntimeException("Module '${import.namespace}' does not exist")

        val source = path.readText()
        val tokens = lex(source)
        val ast = Parser(tokens).parse()

        val moduleEnv = DrEnv(env)

        for (stmt in ast) {
            stmt.eval(moduleEnv)
        }

        when {
            import.alias != null -> env.define(
                import.alias,
                DrModule(import.alias, moduleEnv.export()))

            import.parts != null -> {
                val recorded = mutableSetOf<String>()

                for (part in import.parts) {
                    val partName = part.alias ?: part.source

                    recorded += part.source

                    val sym = moduleEnv.resolve(part.source)
                        ?: moduleEnv.resolveClass(part.source)
                        ?: throw DriftRuntimeException("Symbol ${part.source} not found in '${import.namespace}'")

                    env.define(partName, sym)
                }

                if (import.wildcard) {
                    for ((k, v) in moduleEnv.export().filter { it.key !in recorded }) {
                        when (v) {
                            is DrClass  -> env.defineClass(k, v)
                            else        -> env.define(k, v)
                        }
                    }
                }
            }

            else -> env.define(
                import.steps.last(),
                DrModule(import.namespace, moduleEnv.export()))
        }

        imported.add(import.namespace)
    }
}