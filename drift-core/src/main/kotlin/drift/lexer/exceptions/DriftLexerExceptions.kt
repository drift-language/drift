/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.lexer.exceptions

import drift.exceptions.DriftException
import drift.lexer.Token


/******************************************************************************
 * DRIFT LEXER EXCEPTIONS
 *
 * All Drift Lexer exception classes are defined in this file.
 ******************************************************************************/



/**
 * Drift exception to throw on error on lexing.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
sealed class DriftLexerException(
    val token: Token? = null,
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)



/**
 * Drift Lexer exception to throw if an unexpected character
 * has been found.
 *
 * @property unexpected Unexpected character found
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DLUnexpectedCharacterException(
    unexpected: Char,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftLexerException(
    message = "Unexpected character '$unexpected'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Lexer exception thrown when a string literal is not terminated
 * (closed by its final double-quote).
 *
 * ```drift
 * "hello
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DLUnterminatedStringLiteralException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftLexerException(
    message = "Unterminated string literal",
    sourceName = sourceName,
    line = line,
    pos = pos
)