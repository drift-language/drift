/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime.exceptions


/******************************************************************************
 * DRIFT MODULE LOADER EXCEPTIONS
 *
 * All Drift Module Loader exception classes are defined in this file.
 ******************************************************************************/



sealed class DriftModuleLoaderException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : DriftRuntimeException(
    message = message,
    sourceName = sourceName,
    line = line,
    pos = pos,
)



class DMLAlreadyImportedModuleException(
    moduleNamespace: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : DriftModuleLoaderException(
    message = "Module $moduleNamespace is already imported",
    sourceName = sourceName,
    line = line,
    pos = pos,
)



class DMLUnexistingModuleException(
    moduleNamespace: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : DriftModuleLoaderException(
    message = "Module $moduleNamespace does not exist",
    sourceName = sourceName,
    line = line,
    pos = pos,
)



class DMLNotFoundInModuleException(
    element: String,
    moduleNamespace: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : DriftModuleLoaderException(
    message = "'$element' not found in module $moduleNamespace",
    sourceName = sourceName,
    line = line,
    pos = pos,
)