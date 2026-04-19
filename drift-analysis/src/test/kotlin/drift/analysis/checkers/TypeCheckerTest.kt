/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.checkers

import drift.analysis.exceptions.DTCClassNotFoundException
import drift.analysis.exceptions.DTCInvalidArgsCountException
import drift.analysis.exceptions.DTCRefResolutionNotFoundException
import drift.analysis.exceptions.DTCTypeResolutionNotFoundException
import drift.analysis.exceptions.DTCUnexpectedCalleeException
import drift.analysis.exceptions.DTCUnexpectedReturnStatementException
import drift.analysis.exceptions.DTCUnexpectedTypeException
import drift.analysis.exceptions.DTCUnsupportedIterationException
import drift.analysis.inference.TypeInference
import drift.analysis.symbols.CallableSymbol
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.analysis.symbols.VariableSymbol
import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.Argument
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.Lambda
import drift.ast.expressions.Literal
import drift.ast.expressions.Variable
import drift.ast.statements.Block
import drift.ast.statements.Class
import drift.ast.statements.ExprStmt
import drift.ast.statements.For
import drift.ast.statements.Func
import drift.ast.statements.Let
import drift.ast.statements.ParserStatement
import drift.ast.statements.Return
import drift.runtime.AnyType
import drift.runtime.ObjectType
import drift.runtime.OptionalType
import drift.runtime.UnionType
import drift.runtime.VoidType
import drift.runtime.values.primaries.ParserInt
import drift.runtime.values.primaries.ParserString
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypeCheckerTest {

    @Nested
    inner class LetTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<Int, Int>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()

        private val intClassDeclaration = Class(name = "Int")
        private val intClassSignature = ClassSymbol.ClassSignature(
            name = intClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val intValueType = ObjectType(
            className = intClassDeclaration.name)

        private val stringClassDeclaration = Class(
            name = "String")
        private val stringClassSignature = ClassSymbol.ClassSignature(
            name = stringClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val stringValueType = ObjectType(
            className = stringClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()

            symbolTable.addClass(
                nodeId = intClassDeclaration.nodeId,
                signature = intClassSignature,
                hasPrimaryConstructor = false)

            symbolTable.addClass(
                nodeId = stringClassDeclaration.nodeId,
                signature = stringClassSignature,
                hasPrimaryConstructor = false)
        }



        @Test
        fun `Let with defined type class should not throw`() {
            val expectedType = ObjectType(
                className = intClassDeclaration.name)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = expectedType,
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with defined type class in optional context should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = OptionalType(intValueType),
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with defined type class in union context should not throw`() {
            val secondTypeClass = Class(
                name = "Int64")
            val secondTypeClassSignature = ClassSymbol.ClassSignature(
                name = secondTypeClass.name,
                constructorMethod = CallableSymbol())
            val expectedTypes = listOf(
                ObjectType(className = intClassDeclaration.name),
                ObjectType(className = secondTypeClass.name))
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = UnionType(expectedTypes),
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            symbolTable.addClass(
                nodeId = secondTypeClass.nodeId,
                signature = secondTypeClassSignature,
                hasPrimaryConstructor = false)

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with undefined type class should throw`() {
            val expectedType = ObjectType(
                className = "Unknown")
            val ast: List<ParserStatement> = listOf(
                Let(
                    name = "x",
                    type = expectedType,
                    value = Literal(ParserNotAssigned),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with undefined type class in optional context should throw`() {
            val expectedType = ObjectType(
                className = "Unknown")
            val ast: List<ParserStatement> = listOf(
                Let(
                    name = "x",
                    type = OptionalType(expectedType),
                    value = Literal(ParserNotAssigned),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with undefined type class in union context should throw`() {
            val secondTypeClass = Class(
                name = "Unknown")
            val expectedTypes = listOf(
                intValueType,
                ObjectType(className = secondTypeClass.name))
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = UnionType(expectedTypes),
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Non literal value should not throw`() {
            // NOTE: non literal type returns TRUE

            val fooLet = Let(
                name = "foo",
                type = intValueType,
                value = Literal(ParserInt(1)),
                isMutable = false)
            val fooSignature = VariableSymbol.VariableSignature(
                type = intValueType,
                isMutable = false)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                fooLet,
                Let(
                    name = "x",
                    type = intValueType,
                    value = Variable(fooLet.name),
                    isMutable = false))

            symbolTable.addVariable(
                nodeId = fooLet.nodeId,
                name = fooLet.name,
                signature = fooSignature)

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with type mismatch should throw`() {
            val ast: List<ParserStatement> = listOf(
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = intValueType,
                    value = Literal(ParserString("Hello, Drift!")),
                    isMutable = false))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with type mismatch in optional context should throw`() {
            val ast: List<ParserStatement> = listOf(
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = OptionalType(intValueType),
                    value = Literal(ParserString("Hello, Drift!")),
                    isMutable = false))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with null as value and type in optional context should not throw`() {
            val ast: List<ParserStatement> = listOf(
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = OptionalType(intValueType),
                    value = Literal(ParserNull),
                    isMutable = false))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with union type and unexpected value should throw`() {
            val ast: List<ParserStatement> = listOf(
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = UnionType(listOf(intValueType, stringValueType)),
                    value = Literal(ParserNull),
                    isMutable = false))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with non literal value with resolved type mismatch should throw`() {
            val binary = Binary(
                left = Literal(ParserString("str")),
                operator = "+",
                right = Literal(ParserInt(1)))
            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(binary.nodeId to stringValueType),
                methodResolutions = emptyMap())
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = intValueType,
                    value = binary,
                    isMutable = false))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Let with Any type should never throw`() {
            val ast: List<ParserStatement> = listOf(
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = AnyType,
                    value = Literal(ParserInt(42)),
                    isMutable = false))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }
    }


    @Nested
    inner class FuncTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<Int, Int>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()

        private val intClassDeclaration = Class(name = "Int")
        private val intClassSignature = ClassSymbol.ClassSignature(
            name = intClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val intValueType = ObjectType(className = intClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()

            symbolTable.addClass(
                nodeId = intClassDeclaration.nodeId,
                signature = intClassSignature,
                hasPrimaryConstructor = false)
        }


        @Test
        fun `Func with valid return type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(name = "foo", returnType = intValueType))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with undefined return type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                Func(name = "foo", returnType = ObjectType("Unknown")))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with valid parameter type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(name = "x", type = intValueType))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with undefined parameter type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(name = "x", type = ObjectType("Unknown")))))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with parameter default value matching type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = Literal(ParserInt(0))))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with parameter default value mismatching type should throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = Literal(ParserString("hello"))))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Func with non literal parameter default value with resolved type mismatch should throw`() {
            val binary = Binary(
                left = Literal(ParserString("str")),
                operator = "+",
                right = Literal(ParserInt(1)))
            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(binary.nodeId to ObjectType("String")),
                methodResolutions = emptyMap())
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = binary))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }
    }


    @Nested
    inner class ReturnTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<Int, Int>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()

        private val intClassDeclaration = Class(name = "Int")
        private val intClassSignature = ClassSymbol.ClassSignature(
            name = intClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val intValueType = ObjectType(className = intClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()

            symbolTable.addClass(
                nodeId = intClassDeclaration.nodeId,
                signature = intClassSignature,
                hasPrimaryConstructor = false)
        }


        @Test
        fun `Return outside callable context should throw`() {
            val ast: List<ParserStatement> = listOf(
                Return(value = Literal(ParserInt(1))))

            assertThrows<DTCUnexpectedReturnStatementException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Return inside func with matching type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    returnType = intValueType,
                    body = Block(listOf(
                        Return(value = Literal(ParserInt(1)))))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Return inside func with mismatching type should throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    returnType = intValueType,
                    body = Block(listOf(
                        Return(value = Literal(ParserString("hello")))))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Return inside func with non literal value with resolved type mismatch should throw`() {
            val binary = Binary(
                left = Literal(ParserString("str")),
                operator = "+",
                right = Literal(ParserInt(1)))
            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(binary.nodeId to ObjectType("String")),
                methodResolutions = emptyMap())
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    returnType = intValueType,
                    body = Block(listOf(
                        Return(value = binary)))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }
    }


    @Nested
    inner class CallTests {

        private lateinit var symbolTable: SymbolTable
        private val resolutions = TypeInference.TypeInferenceResult.empty()

        private val intClassDeclaration = Class(name = "Int")
        private val intClassSignature = ClassSymbol.ClassSignature(
            name = intClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val intValueType = ObjectType(className = intClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()

            symbolTable.addClass(
                nodeId = intClassDeclaration.nodeId,
                signature = intClassSignature,
                hasPrimaryConstructor = false)
        }


        @Test
        fun `Call with non-variable callee should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = Literal(ParserInt(1)))))

            assertThrows<DTCUnexpectedCalleeException> {
                TypeChecker(ast, symbolTable, emptyMap(), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with unresolved ref should throw`() {
            val calleeVar = Variable("foo")
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = calleeVar)))

            assertThrows<DTCRefResolutionNotFoundException> {
                TypeChecker(ast, symbolTable, emptyMap(), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with too few args should throw`() {
            val calleeVar = Variable("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSymbol.CallableSignature(
                parameterTypes = listOf(
                    CallableSymbol.CallableSignature.ParameterType(
                        type = intValueType,
                        isRequired = true)))

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = calleeVar, args = emptyList())))

            assertThrows<DTCInvalidArgsCountException> {
                TypeChecker(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with too many args should throw`() {
            val calleeVar = Variable("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSymbol.CallableSignature(
                parameterTypes = emptyList())

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = Literal(ParserInt(1)))))))

            assertThrows<DTCInvalidArgsCountException> {
                TypeChecker(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with wrong arg type should throw`() {
            val calleeVar = Variable("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSymbol.CallableSignature(
                parameterTypes = listOf(
                    CallableSymbol.CallableSignature.ParameterType(
                        type = intValueType,
                        isRequired = true)))

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = Literal(ParserString("hello")))))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with non literal arg with resolved type mismatch should throw`() {
            val calleeVar = Variable("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSymbol.CallableSignature(
                parameterTypes = listOf(
                    CallableSymbol.CallableSignature.ParameterType(
                        type = intValueType,
                        isRequired = true)))

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val binary = Binary(
                left = Literal(ParserString("str")),
                operator = "+",
                right = Literal(ParserInt(1)))
            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(binary.nodeId to ObjectType("String")),
                methodResolutions = emptyMap())
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = binary)))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
                    .check()
            }
        }

        @Test
        fun `Call with valid args should not throw`() {
            val calleeVar = Variable("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSymbol.CallableSignature(
                parameterTypes = listOf(
                    CallableSymbol.CallableSignature.ParameterType(
                        type = intValueType,
                        isRequired = true)))

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = Literal(ParserInt(1)))))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
                    .check()
            }
        }
    }


    @Nested
    inner class ForTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<Int, Int>()

        private val listClassDeclaration = Class(name = "List")
        private val listClassWithIterate = ClassSymbol.ClassSignature(
            name = listClassDeclaration.name,
            constructorMethod = CallableSymbol(),
            methods = linkedMapOf("iterate" to CallableSymbol.CallableSignature()))
        private val listClassWithoutIterate = ClassSymbol.ClassSignature(
            name = listClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val listType = ObjectType(className = listClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
        }


        @Test
        fun `For with valid iterable should not throw`() {
            val iterable = Variable("myList")

            symbolTable.addClass(
                nodeId = listClassDeclaration.nodeId,
                signature = listClassWithIterate,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to listType),
                methodResolutions = emptyMap())

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `For with missing type resolution should throw`() {
            val iterable = Variable("myList")
            val resolutions = TypeInference.TypeInferenceResult.empty()

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCTypeResolutionNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `For with non-ObjectType iterable should throw`() {
            val iterable = Variable("myList")

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to VoidType),
                methodResolutions = emptyMap())

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCUnsupportedIterationException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `For with unregistered iterable class should throw`() {
            val iterable = Variable("myList")

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to ObjectType("Unknown")),
                methodResolutions = emptyMap())

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `For with iterable class missing iterate method should throw`() {
            val iterable = Variable("myList")

            symbolTable.addClass(
                nodeId = listClassDeclaration.nodeId,
                signature = listClassWithoutIterate,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to listType),
                methodResolutions = emptyMap())

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCUnsupportedIterationException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }
    }


    @Nested
    inner class LambdaTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<Int, Int>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()

        private val intClassDeclaration = Class(name = "Int")
        private val intClassSignature = ClassSymbol.ClassSignature(
            name = intClassDeclaration.name,
            constructorMethod = CallableSymbol())
        private val intValueType = ObjectType(className = intClassDeclaration.name)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()

            symbolTable.addClass(
                nodeId = intClassDeclaration.nodeId,
                signature = intClassSignature,
                hasPrimaryConstructor = false)
        }


        @Test
        fun `Lambda with valid return type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(returnType = intValueType)))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with undefined return type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Lambda(returnType = ObjectType("Unknown"))))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with valid parameter type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(name = "x", type = intValueType)))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with undefined parameter type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(name = "x", type = ObjectType("Unknown"))))))

            assertThrows<DTCClassNotFoundException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with parameter default value matching type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = Literal(ParserInt(0)))))))

            assertDoesNotThrow {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with parameter default value mismatching type should throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = Literal(ParserString("hello")))))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }

        @Test
        fun `Lambda with non literal parameter default value with resolved type mismatch should throw`() {
            val binary = Binary(
                left = Literal(ParserString("str")),
                operator = "+",
                right = Literal(ParserInt(1)))
            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(binary.nodeId to ObjectType("String")),
                methodResolutions = emptyMap())
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = binary)))))

            assertThrows<DTCUnexpectedTypeException> {
                TypeChecker(ast, symbolTable, refResolutions, resolutions)
                    .check()
            }
        }
    }
}