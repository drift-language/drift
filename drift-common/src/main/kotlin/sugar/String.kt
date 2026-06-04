/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package sugar

import language.LangInfo


/**
 * @return If the string ends with [LangInfo.FILE_EXTENSION].
 * @author Jonathan (GitHub: belicfr)
 * @see LangInfo.FILE_EXTENSION
 */
fun String.hasDriftExtension() : Boolean =
    endsWith(LangInfo.FILE_EXTENSION)

/**
 * Remove the Drift file extension from
 * the current string.
 *
 * @return The updated string
 * @author Jonathan (GitHub: belicfr)
 * @see LangInfo.FILE_EXTENSION
 */
fun String.removeDriftExtension() : String =
    removeSuffix(LangInfo.FILE_EXTENSION)