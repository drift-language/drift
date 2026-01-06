/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.checkers.collectors.exceptions

import drift.checkers.collectors.SymbolCollector
import drift.exceptions.DriftException


/******************************************************************************
 * DRIFT COLLECTORS (PRE-RUNTIME) EXCEPTIONS
 *
 * All Drift's Collectors exception classes are defined in this file.
 ******************************************************************************/



/**
 * Drift exception to throw on error on the collection (pre-runtime).
 *
 * @property message
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
sealed class DriftCollectorException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftException(message, sourceName, line, pos)



/**
 * Drift Collectors exception to throw on structure definition
 * if a member name is ambiguous (used for another member kind).
 *
 * @property name Member name
 * @property kind Already existing member kind
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DCAmbiguousMemberNameException(
    name: String,
    kind: SymbolCollector.MemberKind,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftCollectorException(
    message = "Ambiguous name: '$name' is already defined as ${kind.label}",
    sourceName = sourceName,
    line = line,
    pos = pos
)