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


sealed class IRException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)



class DIRUnexpectedStatementException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Unexpected statement",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnexpectedExpressionException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Unexpected expression",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnexpectedTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Unexpected type",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRMissingTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Missing type",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnknownRegistryException(
    registry: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Unknown registry '$registry'",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRNotDefinedLambdaCaptureException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Capture of the Lambda '$name' is not defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRNotDefinedSymbolException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Symbol '$name' is not defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRNotDefinedVariableException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Variable '$name' is not defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRNotDefinedCallableException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Function '$name' is not defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRNotDefinedClassException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : IRException(
    message = "Class '$name' is not defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnexpectedNullTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : IRException(
    message = "Unexpected Null usage",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnexpectedVoidTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : IRException(
    message = "Unexpected Void usage",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnexpectedUnknownTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : IRException(
    message = "Unexpected Unknown usage",
    sourceName = sourceName,
    line = line,
    pos = pos
)


class DIRUnsupportedOperationException(
    operator: String,
    types: Pair<ParserType, ParserType?>,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0,
) : IRException(
    message = "Unsupported operation " +
              if (types.second == null) "$operator ${types.first.asString()}"
              else "${types.first.asString()} $operator ${types.second!!.asString()} ",
    sourceName = sourceName,
    line = line,
    pos = pos
)