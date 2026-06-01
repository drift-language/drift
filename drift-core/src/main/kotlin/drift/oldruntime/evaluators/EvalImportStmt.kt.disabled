/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime.evaluators

import drift.ast.statements.Import
import drift.oldruntime.ParserValue
import drift.oldruntime.ModuleLoader
import drift.oldruntime.values.specials.ParserVoid


/******************************************************************************
 * DRIFT IMPORT STATEMENT EVALUATOR
 *
 * This evaluator computes all Drift import statements.
 ******************************************************************************/



fun Import.eval(loader: ModuleLoader) : ParserValue {
    loader.importModule(this)

    return ParserVoid
}