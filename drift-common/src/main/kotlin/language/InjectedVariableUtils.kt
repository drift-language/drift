/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package language

import language.LangInfo.INJECTED_VAR_PREFIX


/**
 * Utility object class for injected variables.
 * 
 * @author Jonathan (GitHub: belicfr)
 */
object InjectedVariableUtils {

    /**
     * Returns a variable name following the injected variable name syntax.
     *
     * @return Injected variable name.
     */
    fun injectedVariable(name: String) : String = "${INJECTED_VAR_PREFIX}$name"

    /**
     * Short version of ``injectedVariable("this")``.
     *
     * @return Injected 'this' variable name.
     * @see injectedVariable
     */
    fun injectedThis() : String = injectedVariable("this")
}