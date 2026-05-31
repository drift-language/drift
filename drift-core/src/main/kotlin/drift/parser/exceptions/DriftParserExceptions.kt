/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.parser.exceptions

import drift.exceptions.DriftException
import drift.lexer.Token


/******************************************************************************
 * DRIFT PARSER EXCEPTIONS
 *
 * All Drift's Parser exception classes are defined in this file.
 ******************************************************************************/



/**
 * Drift exception to throw on error on parsing.
 *
 * @property message
 * @property token The token that caused the exception, if applicable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
sealed class DriftParserException(
    val token: Token? = null,
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)



/**
 * Drift Parser exception to throw if an import statement has been found
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPImportsStatementsMustPrecedeAllOtherStatementsException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Imports must precede all other statements: " +
              "they must be done in top of the file",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Parser exception to throw if a token has been found
 * instead of an expected other.
 *
 * @property expected Expected token description
 * @property found Found token instead of the expected one
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DPMissingExpectedTokenException(
    expected: String,
    found: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Expected $expected" +
              if (found != null) " but found: $found"
              else "",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Parser exception to throw if a token has been found
 * instead of a [Token.NewLine].
 *
 * @property expected Expected token description
 * @property found Found token instead of the expected one
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPExpectedNewlineBetweenTopLevelStatementsException(
    found: Token? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DPMissingExpectedTokenException(
    expected = "newline between top-level statements",
    found = found,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if an unexpected expression
 * has been found.
 *
 * @property unexpected Unexpected expression
 * @property context Context where the expression has been found
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnexpectedExpressionException(
    unexpected: Token? = null,
    context: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Unexpected expression" +
              (if (unexpected == null) "" else " '$unexpected'") +
              " $context",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if an unexpected identifier
 * has been found.
 *
 * @property unexpected Unexpected identifier
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnexpectedIdentifierException(
    unexpected: Token.Identifier,
    context: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Unexpected identifier '${unexpected.value}' $context",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if an unexpected symbol
 * has been found.
 *
 * @property unexpected Unexpected symbol
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnexpectedSymbolException(
    unexpected: Token.Symbol,
    context: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Unexpected symbol '${unexpected.value}' $context",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a parameter name is
 * provided many times in once context.
 *
 * Example:
 * ```drift
 * fun foo(bar, bar) { ... }
 * ```
 *
 * @property parameterName Related parameter's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPParameterAlreadyDefinedException(
    parameterName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = buildMessage(parameterName),
    sourceName = sourceName,
    line = line,
    pos = pos
) {

    companion object {
        private fun buildMessage(parameterName: String): String =
            "Parameter '$parameterName' is already defined"
    }
}



/**
 * Drift's Parser exception to throw if a parameter name is
 * provided many times in once context.
 *
 * Example:
 * ```drift
 * fun foo(bar, bar) { ... }
 * ```
 *
 * @property unallowedName Unallowed hook name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnallowedHookNameException(
    unallowedName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Hook name '$unallowedName' is not allowed",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a named argument has been provided
 * without explicit name labeling.
 *
 * ```drift
 * fun foo(bar) { ... }
 *
 * foo(1)
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPNamedArgumentMustBeNamedException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Named argument must be explicitly named",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a hook that requiring parameter
 * has been defined with no one.
 *
 * @property hookName Related hook's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPMissingHookParameterException(
    hookName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Hook '$hookName' requires parameters",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a return statement
 * has been set in a hook scope.
 *
 * -> Hooks cannot return a value.
 *
 * @property hookName Related hook's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPHookCannotReturnValueException(
    hookName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Hook '$hookName' cannot return a value",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if more than one constructor
 * have been defined in a class.
 *
 * -> Classes can only have one constructor.
 *
 * ```drift
 * class InvalidBodyClass(...) {
 *     init(...) { ... }
 * }
 * class InvalidBodyClassBis {
 *     init(...) { ... }
 *     init(...) { ... }
 * }
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPOnlyOneConstructorPerClassException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "A class cannot have multiple constructors",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a class has more than
 * one static block.
 *
 * -> Classes can only have one static block.
 *
 * ```drift
 * class InvalidBodyClass {
 *     static { ... }
 *     static { ... }
 * }
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPOnlyOneStaticBlockPerClassException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "A class cannot have multiple static blocks",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if an unexpected statement has been
 * found in a class' body.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnexpectedStatementInClassBodyException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Unexpected statement in class body",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if an assignment statement
 * has an invalid value.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPInvalidAssignmentTargetException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Invalid assignment target",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a numeric value is too long,
 * not supported by Drift.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPNumericSizeOverflowException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Too long numeric value",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a branch of a Drift Conditional
 * statement is invalid.
 *
 * @property branchType Conditional branch type
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPInvalidDriftConditionalBranchException(
    branchType: BranchType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = buildMessage(branchType),
    sourceName = sourceName,
    line = line,
    pos = pos
) {

    companion object {
        private fun buildMessage(branchType: BranchType) =
            "Invalid Drift ${branchType.name} branch"
    }


    enum class BranchType {
        IF,
        ELSE,
    }
}



/**
 * Drift's Parser exception to throw if a non-internal variable
 * has '$' as a prefix of its name.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnallowedVariableInjectionPrefixUsageException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "A variable cannot begin with '$'. This character " +
              "is reserved for injected variables.",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a static field of a class
 * is not initialized.
 *
 * ```drift
 * class Foo {
 *     static {
 *         let bar
 *     }
 * }
 * ```
 *
 * @property fieldName Related field's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPStaticFieldMustBeInitializedException(
    fieldName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Static field '$fieldName' must be initialized",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception to throw if a block is not
 * closed (unterminated).
 *
 * ```drift
 * class Foo {
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnterminatedBlockException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = "Unterminated block, expected '}'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception thrown when a type is invalid.
 *
 * @property message
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DPInvalidTypeException(
    message: String? = null,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftParserException(
    message = message ?: "Invalid value type",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception thrown when a union type
 * uses an '?' symbol.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPWrongOptionalUnionTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DPInvalidTypeException(
    message = "Cannot use both '?' and '|' in the same type declaration",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception thrown when a union type
 * unites types with special ones (forbidden).
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPSpecialInUnionTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DPInvalidTypeException(
    message = "Cannot unite special type with another",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift's Parser exception thrown when a union type
 * unites types with special ones (forbidden).
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DPUnsupportedAnnotationException(
    annotationName: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DPInvalidTypeException(
    message = "Cannot use annotation '$annotationName' in this context",
    sourceName = sourceName,
    line = line,
    pos = pos
)