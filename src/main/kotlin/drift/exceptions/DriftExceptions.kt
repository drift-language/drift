package drift.exceptions

import drift.parser.Token

/**
 * Main Drift exception class, inherited
 * from main Kotlin exception class.
 * <p>
 * Must be used as parent for any Drift engine exception.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
sealed class DriftException(
    message: String,
    val token: Token? = null,
    val sourceName: String? = null,
    val line: Int? = null,
    val pos: Int? = null) : Exception(message)


/**
 * Drift exception to throw on error on lexing.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DriftLexerException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, token, sourceName, line, pos)

/**
 * Drift exception to throw on error on parsing.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DriftParserException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, token, sourceName, line, pos)

/**
 * Drift exception to throw on any type error,
 * like casting, implicit typing error, etc.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DriftTypeException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, token, sourceName, line, pos)

/**
 * Drift exception to throw on runtime error.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DriftRuntimeException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, token, sourceName, line, pos)

class DriftSemanticException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, token, sourceName, line, pos)


/**
 * Drift exception to not use outside engine.
 * <p>
 * This exception must be thrown on internal engine error.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DriftInternalException(
    message: String,
    token: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException("Internal error: $message", token, sourceName, line, pos)