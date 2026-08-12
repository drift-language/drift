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
import drift.analysis.symbols.CallableSymbol.CallableSignature
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.Symbol
import drift.analysis.symbols.SymbolTable
import drift.analysis.symbols.VariableSymbol
import drift.ast.NodeId
import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.Argument
import drift.ast.expressions.Binary
import drift.ast.expressions.Call
import drift.ast.expressions.Get
import drift.ast.expressions.Lambda
import drift.ast.expressions.Literal
import drift.ast.expressions.Reference
import drift.ast.statements.Block
import drift.ast.statements.Class
import drift.ast.statements.ExprStmt
import drift.ast.statements.For
import drift.ast.statements.Func
import drift.ast.statements.Let
import drift.ast.statements.ParserStatement
import drift.ast.statements.Return
import drift.oldruntime.AnyType
import drift.oldruntime.ObjectType
import drift.oldruntime.OptionalType
import drift.oldruntime.UnionType
import drift.oldruntime.VoidType
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.primaries.ParserString
import drift.oldruntime.values.primaries.ParserNull
import language.Namespace
import language.QualifiedName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SemanticCheckerTest {

    private val namespace = Namespace("test")

    /** Bundles the pieces needed to declare + register a trivial class in a test. */
    private data class ClassFixture(
        val declaration: Class,
        val qualifiedName: QualifiedName,
        val signature: ClassSymbol.ClassSignature,
        val valueType: ObjectType)

    private fun classFixture(
        name: String,
        methods: LinkedHashMap<String, CallableSignature> = linkedMapOf()
    ): ClassFixture {

        val declaration = Class(name = name)
        val qualifiedName = QualifiedName(namespace, simpleName = name)
        val constructorSignature = CallableSignature(
            scopeType = Symbol.MemberScope(qualifiedName))
        val signature = ClassSymbol.ClassSignature(
            qualifiedName = qualifiedName,
            constructorMethod = CallableSymbol(constructorSignature),
            methods = methods)

        return ClassFixture(
            declaration, qualifiedName, signature,
            valueType = ObjectType(className = name))
    }

    private fun SymbolTable.register(fixture: ClassFixture, hasPrimaryConstructor: Boolean = false) {
        addClass(
            nodeId = fixture.declaration.nodeId,
            signature = fixture.signature,
            hasPrimaryConstructor = hasPrimaryConstructor)
    }

    private fun checkAst(
        ast: List<ParserStatement>,
        symbolTable: SymbolTable,
        refResolutions: Map<NodeId, NodeId> = emptyMap(),
        resolutions: TypeInference.TypeInferenceResult = TypeInference.TypeInferenceResult.empty()
    ) = SemanticChecker(namespace, ast, symbolTable, refResolutions, resolutions).check()

    /** `"str" + 1`, resolved (as if by TypeInference) to [type] — used by the
     *  "non literal value with resolved type mismatch" tests across several node kinds. */
    private fun stringPlusIntWithResolvedType(
        type: ObjectType
    ): Pair<Binary, TypeInference.TypeInferenceResult> {
        val binary = Binary(
            left = Literal(ParserString("str")),
            operator = "+",
            right = Literal(ParserInt(1)))

        return binary to TypeInference.TypeInferenceResult(
            typeResolutions = mapOf(binary.nodeId to type))
    }

    private val intFixture = classFixture("Int")
    private val intClassDeclaration = intFixture.declaration
    private val intValueType = intFixture.valueType

    private val stringFixture = classFixture("String")
    private val stringClassDeclaration = stringFixture.declaration
    private val stringValueType = stringFixture.valueType


    @Nested
    inner class LetTests {

        private lateinit var symbolTable: SymbolTable
        private var refResolutions = mapOf<NodeId, NodeId>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            refResolutions = mapOf()

            symbolTable.register(intFixture)
            symbolTable.register(stringFixture)
        }


        @Test
        fun `Let with defined type class should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = intValueType,
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Let with defined type class in union context should not throw`() {
            val secondTypeFixture = classFixture("Int64")
            val expectedTypes = listOf(intValueType, secondTypeFixture.valueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = UnionType(expectedTypes),
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            symbolTable.register(secondTypeFixture)

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Let with undefined type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                Let(
                    name = "x",
                    type = ObjectType(className = "Unknown"),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Let with undefined type class in optional context should throw`() {
            val ast: List<ParserStatement> = listOf(
                Let(
                    name = "x",
                    type = OptionalType(ObjectType(className = "Unknown")),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Let with undefined type class in union context should throw`() {
            val expectedTypes = listOf(intValueType, ObjectType(className = "Unknown"))
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Let(
                    name = "x",
                    type = UnionType(expectedTypes),
                    value = Literal(ParserInt(1)),
                    isMutable = false))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Non literal value should not throw`() {
            val fooLet = Let(
                name = "foo",
                type = intValueType,
                value = Literal(ParserInt(1)),
                isMutable = false)
            val fooRef = Reference(fooLet.name)
            val fooSignature = VariableSymbol.VariableSignature(
                type = intValueType,
                isMutable = false,
                scopeType = Symbol.LocalScope)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                fooLet,
                Let(name = "x", type = intValueType, value = fooRef, isMutable = false))

            symbolTable.addVariable(
                nodeId = fooLet.nodeId,
                name = fooLet.name,
                signature = fooSignature)

            refResolutions = mapOf(fooRef.nodeId to fooLet.nodeId)

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Let with non literal value with resolved type mismatch should throw`() {
            val (binary, resolutions) = stringPlusIntWithResolvedType(stringValueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                stringClassDeclaration,
                Let(
                    name = "x",
                    type = intValueType,
                    value = binary,
                    isMutable = false))

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }
    }


    @Nested
    inner class FuncTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<NodeId, NodeId>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }


        @Test
        fun `Func with valid return type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(name = "foo", returnType = intValueType))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Func with undefined return type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                Func(name = "foo", returnType = ObjectType("Unknown")))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Func with valid parameter type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(name = "x", type = intValueType))))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Func with undefined parameter type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(name = "x", type = ObjectType("Unknown")))))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Func with non literal parameter default value with resolved type mismatch should throw`() {
            val (binary, resolutions) = stringPlusIntWithResolvedType(stringValueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = binary))))

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }
    }


    @Nested
    inner class ReturnTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<NodeId, NodeId>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }


        @Test
        fun `Return outside callable context should throw`() {
            val ast: List<ParserStatement> = listOf(
                Return(value = Literal(ParserInt(1))))

            assertThrows<DTCUnexpectedReturnStatementException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Return inside func with non literal value with resolved type mismatch should throw`() {
            val (binary, resolutions) = stringPlusIntWithResolvedType(stringValueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                Func(
                    name = "foo",
                    returnType = intValueType,
                    body = Block(listOf(
                        Return(value = binary)))))

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }
    }


    @Nested
    inner class CallTests {

        private lateinit var symbolTable: SymbolTable
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }


        @Test
        fun `Call with non-variable callee should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = Literal(ParserInt(1)))))

            assertThrows<DTCUnexpectedCalleeException> { checkAst(ast, symbolTable) }
        }

        @Test
        fun `Call with unresolved ref should throw`() {
            val calleeVar = Reference("foo")
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = calleeVar)))

            assertThrows<DTCRefResolutionNotFoundException> { checkAst(ast, symbolTable) }
        }

        @Test
        fun `Call with optional parameter omitted`() {
            val param = FunctionParameter(
                name = "a",
                type = intValueType,
                defaultValue = Literal(ParserInt(1)))
            val calleeVar = Reference("foo")
            val funcDecl = Func(
                name = "foo",
                parameters = listOf(param))
            val funcSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = param.name,
                        type = intValueType,
                        isRequired = false)),
                scopeType = Symbol.LocalScope)

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = calleeVar)))

            assertDoesNotThrow {
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }

        @Test
        fun `Call with too few args should throw`() {
            val calleeVar = Reference("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = Symbol.LocalScope)

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(callee = calleeVar)))

            assertThrows<DTCInvalidArgsCountException> {
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }

        @Test
        fun `Call with too many args should throw`() {
            val calleeVar = Reference("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSignature(
                scopeType = Symbol.LocalScope)

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val ast: List<ParserStatement> = listOf(
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = Literal(ParserInt(1)))))))

            assertThrows<DTCInvalidArgsCountException> {
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }

        @Test
        fun `Call with wrong arg type should throw`() {
            val calleeVar = Reference("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = Symbol.LocalScope)

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
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }

        @Test
        fun `Call with non literal arg with resolved type mismatch should throw`() {
            val calleeVar = Reference("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = Symbol.LocalScope)

            symbolTable.addCallable(
                nodeId = funcDecl.nodeId,
                name = funcDecl.name,
                signature = funcSignature)

            val (binary, resolutions) = stringPlusIntWithResolvedType(stringValueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Call(
                    callee = calleeVar,
                    args = listOf(Argument(name = null, expr = binary)))))

            assertThrows<DTCUnexpectedTypeException> {
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }

        @Test
        fun `Call with valid args should not throw`() {
            val calleeVar = Reference("foo")
            val funcDecl = Func(name = "foo")
            val funcSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = Symbol.LocalScope)

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
                checkAst(ast, symbolTable, mapOf(calleeVar.nodeId to funcDecl.nodeId), resolutions)
            }
        }
    }


    @Nested
    inner class MethodCallTests {

        private lateinit var symbolTable: SymbolTable

        private val aClassDeclaration = Class(name = "A")
        private val aClassQualifiedName = QualifiedName(
            namespace,
            simpleName = aClassDeclaration.name)
        private val aValueType = ObjectType(className = "test/${aClassDeclaration.name}")
        private val aClassMemberScope = Symbol.MemberScope(aClassQualifiedName)
        private val anyAClassMethodSignature = CallableSignature(
            scopeType = aClassMemberScope)
        private val aClassSignature = ClassSymbol.ClassSignature(
            qualifiedName = aClassQualifiedName,
            constructorMethod = CallableSymbol(anyAClassMethodSignature),
            methods = linkedMapOf("t" to anyAClassMethodSignature))


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }

        private fun buildMethodCall(methodName: String, args: List<Argument> = emptyList())
            : Triple<Reference, Call, Call> {
            val innerVar = Reference("A")
            val receiverCall = Call(callee = innerVar)
            val outerCall = Call(callee = Get(receiver = receiverCall, name = methodName), args = args)
            return Triple(innerVar, receiverCall, outerCall)
        }


        @Test
        fun `Method call on instance should not throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall("t")

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to aValueType))

            assertDoesNotThrow {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with missing type resolution for receiver should throw`() {
            val (innerVar, _, outerCall) = buildMethodCall("t")

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            assertThrows<DTCTypeResolutionNotFoundException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId))
            }
        }

        @Test
        fun `Method call on non-object type receiver should throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall("t")

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to VoidType))

            assertThrows<DTCUnexpectedCalleeException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with unregistered receiver class should throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall("t")

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to ObjectType("Unknown")))

            assertThrows<DTCClassNotFoundException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with method not found in class should throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall("nonExistent")

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to aValueType))

            assertThrows<DTCRefResolutionNotFoundException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with too few args should throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall("t")

            val methodSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = aClassMemberScope)
            val aClassSignature = ClassSymbol.ClassSignature(
                qualifiedName = aClassQualifiedName,
                constructorMethod = CallableSymbol(anyAClassMethodSignature),
                methods = linkedMapOf("t" to methodSignature))

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to aValueType))

            assertThrows<DTCInvalidArgsCountException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with too many args should throw`() {
            val (innerVar, receiverCall, outerCall) = buildMethodCall(
                "t",
                listOf(Argument(name = null, expr = Literal(ParserInt(1)))))

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to aValueType))

            assertThrows<DTCInvalidArgsCountException> {
                checkAst(
                    listOf(ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }

        @Test
        fun `Method call with wrong arg type should throw`() {
            val arg = Argument(name = null, expr = Literal(ParserString("hello")))
            val (innerVar, receiverCall, outerCall) = buildMethodCall("t", listOf(arg))

            val methodSignature = CallableSignature(
                parameterTypes = listOf(
                    CallableSignature.Parameter(
                        name = "a",
                        type = intValueType,
                        isRequired = true)),
                scopeType = aClassMemberScope)
            val aClassSignature = ClassSymbol.ClassSignature(
                qualifiedName = aClassQualifiedName,
                constructorMethod = CallableSymbol(anyAClassMethodSignature),
                methods = linkedMapOf("t" to methodSignature))

            symbolTable.addClass(
                nodeId = aClassDeclaration.nodeId,
                signature = aClassSignature,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(receiverCall.nodeId to aValueType))

            assertThrows<DTCUnexpectedTypeException> {
                checkAst(
                    listOf(intClassDeclaration, ExprStmt(outerCall)),
                    symbolTable,
                    mapOf(innerVar.nodeId to aClassDeclaration.nodeId),
                    resolutions)
            }
        }
    }


    @Nested
    inner class ForTests {

        private lateinit var symbolTable: SymbolTable
        private var refResolutions = mapOf<NodeId, NodeId>()

        private val myListLet = Let(name = "myList", type = AnyType, isMutable = false)
        private val listClassDeclaration = Class(name = "List")
        private val listClassQualifiedName = QualifiedName(
            namespace,
            simpleName = listClassDeclaration.name)
        private val anyListClassCallableSignature = CallableSignature(
            scopeType = Symbol.MemberScope(listClassQualifiedName))
        private val listClassWithIterate = ClassSymbol.ClassSignature(
            qualifiedName = listClassQualifiedName,
            constructorMethod = CallableSymbol(anyListClassCallableSignature),
            methods = linkedMapOf("iterate" to anyListClassCallableSignature))
        private val listClassWithoutIterate = ClassSymbol.ClassSignature(
            qualifiedName = listClassQualifiedName,
            constructorMethod = CallableSymbol(anyListClassCallableSignature))
        private val listType = ObjectType(className = listClassQualifiedName.qualifiedName)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            refResolutions = mapOf()
        }


        @Test
        fun `For with valid iterable should not throw`() {
            val iterable = Reference("myList")
            refResolutions = mapOf(iterable.nodeId to myListLet.nodeId)

            symbolTable.addClass(
                nodeId = listClassDeclaration.nodeId,
                signature = listClassWithIterate,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to listType))

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `For with missing type resolution should throw`() {
            val iterable = Reference("myList")
            refResolutions = mapOf(iterable.nodeId to myListLet.nodeId)

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCTypeResolutionNotFoundException> { checkAst(ast, symbolTable, refResolutions) }
        }

        @Test
        fun `For with non-ObjectType iterable should throw`() {
            val iterable = Reference("myList")
            refResolutions = mapOf(iterable.nodeId to myListLet.nodeId)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to VoidType))

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCUnsupportedIterationException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `For with unregistered iterable class should throw`() {
            val iterable = Reference("myList")
            refResolutions = mapOf(iterable.nodeId to myListLet.nodeId)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to ObjectType("Unknown")))

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `For with iterable class missing iterate method should throw`() {
            val iterable = Reference("myList")
            refResolutions = mapOf(iterable.nodeId to myListLet.nodeId)

            symbolTable.addClass(
                nodeId = listClassDeclaration.nodeId,
                signature = listClassWithoutIterate,
                hasPrimaryConstructor = false)

            val resolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(iterable.nodeId to listType))

            val ast: List<ParserStatement> = listOf(
                For(iterable = iterable, variables = emptyList(), body = Block.empty()))

            assertThrows<DTCUnsupportedIterationException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }
    }


    @Nested
    inner class LambdaTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<NodeId, NodeId>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }


        @Test
        fun `Lambda with valid return type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(returnType = intValueType)))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Lambda with undefined return type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Lambda(returnType = ObjectType("Unknown"))))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Lambda with valid parameter type should not throw`() {
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(name = "x", type = intValueType)))))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Lambda with undefined parameter type class should throw`() {
            val ast: List<ParserStatement> = listOf(
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(name = "x", type = ObjectType("Unknown"))))))

            assertThrows<DTCClassNotFoundException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, resolutions) }
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

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Lambda with non literal parameter default value with resolved type mismatch should throw`() {
            val (binary, resolutions) = stringPlusIntWithResolvedType(stringValueType)
            val ast: List<ParserStatement> = listOf(
                intClassDeclaration,
                ExprStmt(Lambda(
                    parameters = listOf(
                        FunctionParameter(
                            name = "x",
                            type = intValueType,
                            defaultValue = binary)))))

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, resolutions) }
        }
    }


    @Nested
    inner class ClassTests {

        private lateinit var symbolTable: SymbolTable
        private val refResolutions = mapOf<NodeId, NodeId>()
        private val resolutions = TypeInference.TypeInferenceResult.empty()


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            symbolTable.register(intFixture)
        }


        @Test
        fun `Class field with undefined type should throw`() {
            val clazz = Class(
                name = "Foo",
                fields = mutableListOf(
                    Let(name = "x", type = ObjectType("Unknown"), isMutable = false)))

            assertThrows<DTCClassNotFoundException> { checkAst(listOf(clazz), symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Class static field with undefined type should throw`() {
            val clazz = Class(
                name = "Foo",
                staticFields = mutableListOf(
                    Let(name = "count", type = ObjectType("Unknown"), isMutable = false)))

            assertThrows<DTCClassNotFoundException> { checkAst(listOf(clazz), symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Class method with undefined return type should throw`() {
            val clazz = Class(
                name = "Foo",
                methods = mutableListOf(
                    Func(name = "get", returnType = ObjectType("Unknown"))))

            assertThrows<DTCClassNotFoundException> { checkAst(listOf(clazz), symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Class static method with undefined return type should throw`() {
            val clazz = Class(
                name = "Foo",
                staticMethods = mutableListOf(
                    Func(name = "create", returnType = ObjectType("Unknown"))))

            assertThrows<DTCClassNotFoundException> { checkAst(listOf(clazz), symbolTable, refResolutions, resolutions) }
        }

        @Test
        fun `Class with valid field and method types should not throw`() {
            val clazz = Class(
                name = "Foo",
                fields = mutableListOf(
                    Let(name = "x", type = intValueType, isMutable = false)),
                methods = mutableListOf(
                    Func(name = "get", returnType = intValueType)),
                staticFields = mutableListOf(
                    Let(name = "count", type = intValueType, isMutable = false)),
                staticMethods = mutableListOf(
                    Func(name = "create", returnType = intValueType)))

            assertDoesNotThrow {
                checkAst(listOf(intClassDeclaration, clazz), symbolTable, refResolutions, resolutions)
            }
        }
    }


    @Nested
    inner class ImportTests {

        private lateinit var symbolTable: SymbolTable
        private var refResolutions = mapOf<NodeId, NodeId>()

        private val importedLet = Let(name = "myValue", type = AnyType, isMutable = false)


        @BeforeEach
        fun setUp() {
            symbolTable = SymbolTable()
            refResolutions = mapOf()

            symbolTable.register(intFixture)
            symbolTable.register(stringFixture)
            symbolTable.addVariable(
                nodeId = importedLet.nodeId,
                name = "main/myValue",
                signature = VariableSymbol.VariableSignature(
                    intValueType,
                    false,
                    Symbol.TopLevelScope(Namespace("main"))))
        }


        @Test
        fun `Reference to imported symbol with valid resolution should not throw`() {
            val ref = Reference("myValue")
            refResolutions = mapOf(ref.nodeId to importedLet.nodeId)

            assertDoesNotThrow { checkAst(listOf(ExprStmt(ref)), symbolTable, refResolutions) }
        }

        @Test
        fun `Reference to unresolved import should throw`() {
            val ref = Reference("notImported")

            assertThrows<DTCRefResolutionNotFoundException> { checkAst(listOf(ExprStmt(ref)), symbolTable) }
        }

        @Test
        fun `Reference to aliased imported symbol should not throw`() {
            val alias = Reference("mv")
            refResolutions = mapOf(alias.nodeId to importedLet.nodeId)

            assertDoesNotThrow { checkAst(listOf(ExprStmt(alias)), symbolTable, refResolutions) }
        }

        @Test
        fun `Let with imported value matching declared type should not throw`() {
            val ref = Reference("myValue")
            refResolutions = mapOf(ref.nodeId to importedLet.nodeId)
            val typeResolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(ref.nodeId to intValueType))

            val ast: List<ParserStatement> = listOf(
                Let(name = "x", type = intValueType, value = ref, isMutable = false))

            assertDoesNotThrow { checkAst(ast, symbolTable, refResolutions, typeResolutions) }
        }

        @Test
        fun `Let with imported value mismatching declared type should throw`() {
            val ref = Reference("myValue")
            refResolutions = mapOf(ref.nodeId to importedLet.nodeId)
            val typeResolutions = TypeInference.TypeInferenceResult(
                typeResolutions = mapOf(ref.nodeId to intValueType))

            val ast: List<ParserStatement> = listOf(
                Let(name = "x", type = stringValueType, value = ref, isMutable = false))

            assertThrows<DTCUnexpectedTypeException> { checkAst(ast, symbolTable, refResolutions, typeResolutions) }
        }

        @Test
        fun `Get on accessor import receiver with valid resolution should not throw`() {
            val moduleLet = Let(name = "users", type = AnyType, isMutable = false)
            val moduleRef = Reference("users")
            refResolutions = mapOf(moduleRef.nodeId to moduleLet.nodeId)

            val get = Get(receiver = moduleRef, name = "MyClass")

            assertDoesNotThrow { checkAst(listOf(ExprStmt(get)), symbolTable, refResolutions) }
        }

        @Test
        fun `Get on accessor import with unresolved receiver should throw`() {
            val moduleRef = Reference("users")
            val get = Get(receiver = moduleRef, name = "MyClass")

            assertThrows<DTCRefResolutionNotFoundException> { checkAst(listOf(ExprStmt(get)), symbolTable) }
        }
    }
}
