/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.exceptions


/******************************************************************************
 * DRIFT INTERNAL EXCEPTION CLASSES
 *
 * All Drift engine exception classes are defined in this file.
 ******************************************************************************/



/**
 * Main Drift exception class, inherited
 * from the main Kotlin exception class.
 *
 * Must be used as a parent for any Drift engine exception.
 *
 * @property message
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DriftException(
    message: String,
    val sourceName: String? = null,
    val line: Int? = null,
    val pos: Int? = null
) : Exception(buildMessage(message, line, pos)) {

    companion object {
        private fun buildMessage(message: String, line: Int?, pos: Int?): String =
            message +
            if (line == null || pos == null) ""
            else "\nLine: $line\nPosition: $pos"
    }
}