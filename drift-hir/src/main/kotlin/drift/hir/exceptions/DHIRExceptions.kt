/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.hir.exceptions

import drift.exceptions.DriftException

sealed class DHIRException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)


class DHIRUnsupported(
    message: String = "Unsupported operation",
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DHIRException(
    message = message,
    sourceName = sourceName,
    line = line,
    pos = pos
)