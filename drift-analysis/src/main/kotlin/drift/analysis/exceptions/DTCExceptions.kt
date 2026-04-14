/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.exceptions

import drift.exceptions.DriftException
import drift.runtime.ParserType


sealed class DTCException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)


class DTCUnexpectedTypeException(
    expected: ParserType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "Expected type '${expected.asString()}' in this context",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCUnexpectedReturnStatementException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "A return statement was found outside of " +
              "a function definition or lambda expression",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCCannotReturnValueInNonReturnableContextException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "This callable cannot return a value",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCTypeResolutionNotFoundException(
    nodeId: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "Failed to resolve type for AST node #$nodeId",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCUnsupportedIterationException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "This type does not support iteration",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCClassNotFoundException(
    className: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "Class '$className' not found",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCUnexpectedCalleeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "Unexpected callee",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCRefResolutionNotFoundException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "Undefined callee",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DTCInvalidArgsCountException(
    expected: Int,
    given: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DTCException(
    message = "This call must have $expected arguments, " +
              "not $given",
    sourceName = sourceName,
    line = line,
    pos = pos
)