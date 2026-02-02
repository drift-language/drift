/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.evaluators

import drift.ast.statements.Import
import drift.runtime.ParserValue
import drift.runtime.ModuleLoader
import drift.runtime.values.specials.ParserVoid


/******************************************************************************
 * DRIFT IMPORT STATEMENT EVALUATOR
 *
 * This evaluator computes all Drift import statements.
 ******************************************************************************/



fun Import.eval(loader: ModuleLoader) : ParserValue {
    loader.importModule(this)

    return ParserVoid
}