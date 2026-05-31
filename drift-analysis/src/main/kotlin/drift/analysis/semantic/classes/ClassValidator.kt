/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.semantic.classes

import drift.analysis.semantic.SemanticValidator
import drift.ast.statements.Class
import drift.ast.statements.Func
import drift.ast.statements.Let
import drift.lexer.Token


class ClassValidator(val `class`: Class) : SemanticValidator {

    fun validate() {
        `Class should not have duplicate fields or methods`()
        `Class must always have at least one init hook (constructor)`()
    }


    /* RULES */

    private fun `Class should not have duplicate fields or methods`() {
        fun verifyFieldDuplicates(source: List<Let>) {
            source.forEach { field ->
                if (source.count { it.name == field.name } > 1)
                    error("error duplicate field name '${field.name}'")
            }
        }
        fun verifyMethodDuplicates(source: List<Func>) {
            source.forEach { field ->
                if (source.count { it.name == field.name } > 1)
                    error("error duplicate field name '${field.name}'")
            }
        }


        `class`.let {
            verifyFieldDuplicates(it.fields)
            verifyFieldDuplicates(it.staticFields)
            verifyMethodDuplicates(it.methods)
            verifyMethodDuplicates(it.staticMethods)
        }
    }

    private fun `Class must always have at least one init hook (constructor)`() {
        if (!`class`.hookExists(Token.Keyword.INIT.value))
            error("error missing class constructor")
    }
}