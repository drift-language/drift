/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2025. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.runtime.exceptions

import drift.exceptions.DriftException
import drift.runtime.DrType
import drift.runtime.DrValue


/******************************************************************************
 * DRIFT RUNTIME EXCEPTIONS
 *
 * All Drift's Runtime exception classes are defined in this file.
 ******************************************************************************/



/**
 * Drift exception thrown when on runtime error.
 *
 * @property message
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DriftRuntimeException(
    message: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0)
    : DriftException(message, sourceName, line, pos)



/**
 * Drift Runtime exception thrown when the provided type
 * is not supported in the related context.
 *
 * @property type Related type
 * @property message
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRNotSupportedTypeException(
    type: DrType,
    message: String = "${type.asString()} is not supported in the current context.",
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when the provided type
 * is not supported in a Range.
 *
 * @property type Related type
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRNotSupportedTypeInRangeException(
    type: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRNotSupportedTypeException(
    type = type,
    message = "${type.asString()} is not supported in a Range context.",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when the provided expression's type
 * is not boolean in a condition context.
 *
 * @property type Related type
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRNotSupportedTypeInBooleanExpressionException(
    type: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRNotSupportedTypeException(
    type = type,
    message = "A condition must be Boolean",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when the provided types
 * are different and not supported in the current context.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRRangeLimitsMustHaveSameTypeException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Both Range limits must have the same type",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unassigned
 * entity is used as a value.
 *
 * ```drift
 * let a
 *
 * print(a)
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotUseUnassignedEntityException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot use unassigned entity",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when Void
 * is used as a value.
 *
 * ```drift
 * fun foo { }
 *
 * print(foo())
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotUseVoidAsValueException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot use Void as a value",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a cast
 * is unsuccessful: a type cannot be cast to another.
 *
 * @property valueType
 * @property expectedType
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnsuccessfulCastException(
    valueType: DrType,
    expectedType: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Unsuccessful casting ${valueType.asString()} to ${expectedType.asString()}",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an entity is already defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRAlreadyDefinedException(
    message: String? = null,
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message ?: "'$name' is already defined",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a class is already defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRClassAlreadyDefinedException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRAlreadyDefinedException(
    message = "Class '$name' is already defined",
    name = name,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a variable is already defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRVariableAlreadyDefinedException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRAlreadyDefinedException(
    message = "Variable '$name' is already defined",
    name = name,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when none entity is defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRNotDefinedException(
    message: String? = null,
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message ?: "'$name' does not exist",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when none class is defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRClassNotDefinedException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRNotDefinedException(
    message = "Class '$name' does not exist",
    name = name,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when none variable is defined
 * with the provided name.
 *
 * @property name Entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRVariableNotDefinedException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRNotDefinedException(
    message = "Variable '$name' does not exist",
    name = name,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a wrong number of arguments
 * have been provided on a call.
 *
 * @property message
 * @property expected Expected arguments count
 * @property actual Actual arguments count
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRWrongNumberOfArgumentsException(
    message: String? = null,
    expected: Int,
    actual: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message ?: "Wrong number of arguments\nExpected: $expected\nActual: $actual",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a wrong number of arguments
 * have been provided on a class call.
 *
 * @property className Class' name
 * @property message
 * @property expected Expected arguments count
 * @property actual Actual arguments count
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRWrongNumberOfClassArgumentsException(
    className: String,
    expected: Int,
    actual: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRWrongNumberOfArgumentsException(
    message = "Wrong number of arguments for class '$className'\nExpected: $expected\nActual: $actual",
    expected = expected,
    actual = actual,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when too many arguments have
 * been provided regarding the expected count.
 *
 * @property message
 * @property expected Expected arguments count
 * @property actual Actual arguments count
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRTooManyArgumentsException(
    message: String? = null,
    expected: Int,
    actual: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message ?: "Too many arguments.\nExpected: $expected\nActual: $actual",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when too many positional arguments have
 * been provided regarding the expected count.
 *
 * @property expected Expected arguments count
 * @property actual Actual arguments count
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRTooManyPositionalArgumentsException(
    expected: Int,
    actual: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRTooManyArgumentsException(
    message = "Too many positional arguments.\nExpected: $expected\nActual: $actual",
    expected = expected,
    actual = actual,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a positional argument appears
 * below a named one.
 *
 * Drift enforces that positional arguments must appear before
 * named arguments.
 *
 * ```drift
 * fun foo(*bar, foo2) { ... }
 *
 * foo(2, foo2 = 1)     // GOOD
 * foo(foo2 = 1, 2)     // WRONG
 * ```
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRPositionalMustPrecedeNamedArgumentsException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Positional arguments must appear before named arguments.",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when positional arguments are used
 * in a context that forbids them.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRPositionalArgumentsNotAllowedException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Positional arguments are not allowed in this context",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when named arguments are used
 * in a context that forbids them.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRNamedArgumentsNotAllowedException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Named arguments are not allowed in this context",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an argument is already bound
 * (duplicate argument in one call).
 *
 * @property name Parameter's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRArgumentAlreadyBoundException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Argument already bound to '$name' parameter",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unknown parameter name is
 * given on a named argument.
 *
 * @property name Parameter's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnknownParameterException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Unknown parameter '$name'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a parameter does not
 * have any value on a call.
 *
 * @property name Parameter's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRMissingArgumentException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Missing argument for parameter '$name'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a non-callable is invoked.
 *
 * @property name Non-callable entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRNonCallableInvocationException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot invoke non-callable '$name'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unsupported operator
 * has been used.
 *
 * @property operator Used operator
 * @property types Types of both operands
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnsupportedOperatorException(
    operator: String,
    types: Pair<DrType, DrType>,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Unsupported operator '$operator' for types ${types.first.asString()} and ${types.second.asString()}",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a division by zero
 * has been found. A division by zero is impossible.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRDivisionByZeroException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Division by zero is not allowed",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unknown operator has
 * been used.
 *
 * @property operator Unknown operator
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnknownOperatorException(
    operator: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Unknown operator '$operator'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a non-negatable expression
 * is negated.
 *
 * Example:
 * ```drift
 * !"hello"
 * ```
 *
 * @property type Type of the negated expression
 * @property operator Used operator to negate
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotNegateException(
    type: DrType,
    operator: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot negate value of type (${type.asString()}) using '$operator' operator",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unsigned value is negated.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotNegateUnsignedException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot negate unsigned",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unknown member has been
 * called from a class.
 *
 * ```drift
 * A().unknownMember
 * ```
 *
 * @property message
 * @property memberName Unknown member's name
 * @property className Class' name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
open class DRUnknownClassMemberException(
    message: String? = null,
    memberName: String,
    className: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = message ?: "Unknown member '$memberName' in class '$className'",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unknown member
 * has been called statically from a class.
 *
 * ```drift
 * A.unknownMember
 * ```
 *
 * @property memberName Unknown member's name
 * @property className Class' name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnknownClassStaticMemberException(
    memberName: String,
    className: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DRUnknownClassMemberException(
    message = "Unknown static member '$memberName' in class '$className'",
    memberName = memberName,
    className = className,
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a member-access has been
 * done on a non-object value. In Drift, most of the types are
 * objects, including most of the primitives.
 *
 * For example, Null is not an object.
 *
 * @property valueType Type of the non-object value
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRNotAnObjectException(
    valueType: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "'${valueType.asString()}' is not an object.",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an unsettable object
 * has been accessed to set a member's value.
 *
 * ```drift
 * "hello".member = 1
 * ```
 * TODO: verify example
 *
 * @property type Type of the object
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotSetObjectException(
    type: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot set on '${type.asString()}' object",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an invalid
 * statement has been found.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRInvalidStatementException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Invalid statement",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when an invalid
 * expression has been found.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRInvalidExpressionException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Invalid expression",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a return statement is expected
 * but not found.
 *
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRMissingReturnStatementException(
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Missing return statement",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a non-iterable value is used
 * as an iterator.
 *
 * @property type Type of the non-iterable value
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotIterateException(
    type: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot iterate over ${type.asString()}",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a destructuring operation is
 * invalid.
 *
 * Example:
 * - Too many sub-variables
 * - Value is not destructurable
 *
 * @property type Type of the non-iterable value
 * @property variablesCount Count of sub-variables
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotDestructureException(
    type: DrType,
    variablesCount: Int,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot destructure ${type.asString()} value " +
              "into $variablesCount variables",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when a variable cannot be
 * reassigned with the provided value.
 *
 * @property newValueType Type of the new value
 * @property type Type of the variable
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRUnassignableException(
    newValueType: DrType,
    type: DrType,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot assign ${newValueType.asString()} to a ${type.asString()} variable",
    sourceName = sourceName,
    line = line,
    pos = pos
)



/**
 * Drift Runtime exception thrown when attempting to
 * assign to an immutable variable.
 *
 * Example:
 * ```drift
 * let a = 1
 * a = 2
 * ```
 *
 * @property name Immutable entity's name
 * @property sourceName The file where the exception has been thrown
 * @property line The line where the error is
 * @property pos The character position where the error is
 */
class DRCannotAssignToImmutableException(
    name: String,
    sourceName: String? = null,
    line: Int = 0,
    pos: Int = 0
) : DriftRuntimeException(
    message = "Cannot assign to immutable $name",
    sourceName = sourceName,
    line = line,
    pos = pos
)