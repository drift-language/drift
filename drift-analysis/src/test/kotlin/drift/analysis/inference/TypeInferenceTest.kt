/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.analysis.inference

import drift.analysis.exceptions.DIRNotDefinedSymbolException
import drift.analysis.exceptions.DIRUnexpectedExpressionException
import drift.analysis.exceptions.DIRUnexpectedTypeException
import drift.analysis.exceptions.DIRUnexpectedUnknownTypeException
import drift.analysis.exceptions.DIRUnexpectedVoidTypeException
import drift.analysis.exceptions.DIRUnsupportedOperationException
import drift.analysis.symbols.CallableSymbol
import drift.analysis.symbols.ClassSymbol
import drift.analysis.symbols.SymbolTable
import drift.analysis.symbols.VariableSymbol
import drift.ast.expressions.*
import drift.ast.statements.*
import drift.runtime.*
import drift.runtime.values.primaries.*
import drift.runtime.values.specials.ParserNotAssigned
import drift.runtime.values.specials.ParserNull
import drift.runtime.values.specials.ParserVoid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.collections.mapOf

class TypeInferenceTest {

    private val intOT = ObjectType(ParserPrimitiveClass.Int.className)
    private val int64OT = ObjectType(ParserPrimitiveClass.Int64.className)
    private val boolOT = ObjectType(ParserPrimitiveClass.Bool.className)
    private val stringOT = ObjectType(ParserPrimitiveClass.String.className)

    private val intSample = Literal(ParserInt(1))
    private val int64Sample = Literal(ParserInt64(2L))
    private val stringSample = Literal(ParserString("Hello, Drift!"))
    private val boolSample = Literal(ParserBool(true))


    @Nested
    inner class LiteralTests {

        val symbolTable = SymbolTable()
        val refResolutions = emptyMap<Int, Int>()


        @Test
        fun `Integer literal should infers ObjectType(Int)`() {
            val literal = Literal(ParserInt(1))
            val ast: List<ParserStatement> = listOf(
                ExprStmt(literal)
            )

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[literal.nodeId],
                ObjectType(ParserPrimitiveClass.Int.className))
        }

        @Test
        fun `64-bits integer literal should infers ObjectType(Int64)`() {
            val literal = Literal(ParserInt64(1L))
            val ast: List<ParserStatement> = listOf(
                ExprStmt(literal)
            )

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[literal.nodeId],
                ObjectType(ParserPrimitiveClass.Int64.className))
        }

        @Test
        fun `Unsigned integer literal should infers ObjectType(UInt)`() {
            val literal = Literal(ParserUInt(1u))
            val ast: List<ParserStatement> = listOf(
                ExprStmt(literal)
            )

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[literal.nodeId],
                ObjectType(ParserPrimitiveClass.UInt.className))
        }

        @Test
        fun `String literal should infers ObjectType(String)`() {
            val literal = Literal(ParserString("Hello, Drift!"))
            val ast: List<ParserStatement> = listOf(
                ExprStmt(literal)
            )

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[literal.nodeId],
                ObjectType(ParserPrimitiveClass.String.className))
        }

        @Test
        fun `Boolean literal should infers ObjectType(Bool)`() {
            val literal = Literal(ParserBool(true))
            val ast: List<ParserStatement> = listOf(
                ExprStmt(literal)
            )

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[literal.nodeId],
                ObjectType(ParserPrimitiveClass.Bool.className))
        }
    }

    @Nested
    inner class ArrayTests {

        private val symbolTable = SymbolTable()
        private val refResolutions = emptyMap<Int, Int>()

        @Test
        fun `Empty Array should return Array(AnyType)`() {
            val array = drift.ast.expressions.Array()

            val ast = listOf<ParserStatement>(ExprStmt(array))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                ArrayType(type = AnyType),
                inference.typeResolutions[array.nodeId])
        }

        @Test
        fun `Homogeneous Array should return Array(type)`() {
            val array = drift.ast.expressions.Array(listOf(
                stringSample,
                stringSample))

            val ast = listOf<ParserStatement>(ExprStmt(array))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                ArrayType(type = stringOT),
                inference.typeResolutions[array.nodeId])
        }

        @Test
        fun `Heterogeneous Array should throw`() {
            val array = drift.ast.expressions.Array(listOf(
                stringSample,
                intSample))

            val ast = listOf<ParserStatement>(ExprStmt(array))

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }
    }

    @Nested
    inner class LetTests {

        val symbolTable = SymbolTable()
        val refResolutions = emptyMap<Int, Int>()


        @Test
        fun `Let with Any as type should infer value type`() {
            val let = Let(
                name = "foo",
                type = AnyType,
                value = Literal(ParserInt(1)),
                isMutable = true)

            val ast: List<ParserStatement> = listOf(let)

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[let.nodeId],
                ObjectType(ParserPrimitiveClass.Int.className))
        }

        @Test
        fun `Let with explicit typing should keep its type`() {
            val let = Let(
                name = "foo",
                type = ObjectType(ParserPrimitiveClass.String.className),
                value = Literal(ParserString("Hello, Drift!")),
                isMutable = true)

            val ast: List<ParserStatement> = listOf(let)

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                inference.typeResolutions[let.nodeId],
                ObjectType(ParserPrimitiveClass.String.className))
        }
    }

    @Nested
    inner class CallableTests {

        @Nested
        inner class FunctionTests {

            val symbolTable = SymbolTable()
            val refResolutions = emptyMap<Int, Int>()


            @Test
            fun `Function without explicit typing and return statement should return VoidType`() {
                /**
                 * ```
                 * fun foo { }
                 * ```
                 */
                val function = Func(name = "foo")

                val ast: List<ParserStatement> = listOf(function)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    inference.typeResolutions[function.nodeId],
                    VoidType)
            }

            @Test
            fun `Function without explicit typing and returning literal integer should return ObjectType(Int)`() {
                /**
                 * ```
                 * fun foo {
                 *   return 42
                 * }
                 * ```
                 */
                val function = Func(
                    name = "foo",
                    body = Block(listOf(
                        Return(Literal(ParserInt(42))))))

                val ast: List<ParserStatement> = listOf(function)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    inference.typeResolutions[function.nodeId],
                    ObjectType(ParserPrimitiveClass.Int.className))
            }

            @Test
            fun `Function without explicit typing and multiple return statements should return UnionType()`() {
                val return42 = Return(Literal(ParserInt(42)))
                val returnBar = Return(Literal(ParserString("Bar")))
                val `if` = If(
                    Literal(ParserBool(true)),
                    Block(listOf(return42)),
                    Block(listOf(returnBar)))

                /**
                 * ```
                 * fun foo {
                 *   if (true) {
                 *     return 42
                 *   } else {
                 *     return "Bar"
                 *   }
                 * }
                 * ```
                 */
                val function = Func(
                    name = "foo",
                    body = Block(listOf(`if`)))

                val ast: List<ParserStatement> = listOf(function)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    UnionType(listOf(
                        ObjectType(ParserPrimitiveClass.Int.className),
                        ObjectType(ParserPrimitiveClass.String.className))),
                    inference.typeResolutions[function.nodeId])
            }

            @Test
            fun `Function with explicit typing and return statement should return good type`() {
                /**
                 * ```
                 * fun foo : Int {
                 *   return 42
                 * }
                 * ```
                 */
                val function = Func(
                    name = "foo",
                    returnType = ObjectType(ParserPrimitiveClass.Int.className),
                    body = Block(listOf(
                        Return(Literal(ParserInt(42))))))

                val ast: List<ParserStatement> = listOf(function)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    inference.typeResolutions[function.nodeId],
                    ObjectType(ParserPrimitiveClass.Int.className))
            }
        }

        @Nested
        inner class LambdaTests {

            val symbolTable = SymbolTable()
            val refResolutions = emptyMap<Int, Int>()


            @Test
            fun `Lambda without explicit typing and return statement should return VoidType`() {
                /**
                 * ```
                 * () -> { }
                 * ```
                 */
                val lambda = Lambda()

                val ast: List<ParserStatement> = listOf(ExprStmt(lambda))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    FunctionType(returnType = VoidType),
                    inference.typeResolutions[lambda.nodeId])
            }

            @Test
            fun `Lambda without explicit typing and returning literal integer should return ObjectType(Int)`() {
                /**
                 * ```
                 * () -> {
                 *   return 42
                 * }
                 * ```
                 */
                val lambda = Lambda(
                    body = Block(listOf(
                        Return(Literal(ParserInt(42))))))

                val ast: List<ParserStatement> = listOf(ExprStmt(lambda))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    FunctionType(
                        returnType = ObjectType(ParserPrimitiveClass.Int.className)),
                    inference.typeResolutions[lambda.nodeId])
            }

            @Test
            fun `Lambda without explicit typing and multiple return statements should return UnionType()`() {
                val return42 = Return(Literal(ParserInt(42)))
                val returnBar = Return(Literal(ParserString("Bar")))
                val `if` = If(
                    Literal(ParserBool(true)),
                    Block(listOf(return42)),
                    Block(listOf(returnBar)))

                /**
                 * ```
                 * () -> {
                 *   if (true) {
                 *     return 42
                 *   } else {
                 *     return "Bar"
                 *   }
                 * }
                 * ```
                 */
                val lambda = Lambda(
                    body = Block(listOf(`if`)))

                val ast: List<ParserStatement> = listOf(ExprStmt(lambda))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                val unionType = UnionType(listOf(
                    ObjectType(ParserPrimitiveClass.Int.className),
                    ObjectType(ParserPrimitiveClass.String.className)))

                assertEquals(
                    FunctionType(returnType = unionType),
                    inference.typeResolutions[lambda.nodeId])
            }

            @Test
            fun `Lambda with explicit typing and return statement should return good type`() {
                /**
                 * ```
                 * () : Int -> {
                 *   return 42
                 * }
                 * ```
                 */
                val lambda = Lambda(
                    returnType = ObjectType(ParserPrimitiveClass.Int.className),
                    body = Block(listOf(
                        Return(Literal(ParserInt(42))))))

                val ast: List<ParserStatement> = listOf(ExprStmt(lambda))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    FunctionType(
                        returnType = ObjectType(ParserPrimitiveClass.Int.className)),
                    inference.typeResolutions[lambda.nodeId])
            }

            @Test
            fun `Lambda with special Last return type should return last expression type`() {
                val one = ExprStmt(Literal(ParserInt(1)))
                val foo = ExprStmt(Literal(ParserString("foo")))

                /**
                 * ```
                 * () : Last -> {
                 *     1
                 *     "foo"
                 * }
                 * ```
                 */
                val lambda = Lambda(
                    returnType = LastType,
                    body = Block(listOf(one, foo)))

                val ast: List<ParserStatement> = listOf(ExprStmt(lambda))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                        FunctionType(returnType = ObjectType(ParserPrimitiveClass.String.className)),
                        inference.typeResolutions[lambda.nodeId])
            }
        }
    }

    @Nested
    inner class CallTests {

        @Nested
        inner class CallableContextTests {

            @Test
            fun `Function returning explicitly an integer should return ObjectType(Int)`() {
                val function = Func(
                    name = "foo",
                    returnType = ObjectType(ParserPrimitiveClass.Int.className))
                val variable = Variable(name = function.name)
                val call = Call(callee = variable)
                val ast: List<ParserStatement> = listOf(function, ExprStmt(call))
                val symbol = CallableSymbol()

                val symbolTable = SymbolTable(mutableMapOf(
                    function.nodeId to symbol))
                val refResolutions = mapOf(variable.nodeId to function.nodeId)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    ObjectType(ParserPrimitiveClass.Int.className),
                    inference.typeResolutions[call.nodeId])
            }

            @Test
            fun `Function returning implicitly an integer should return ObjectType(Int)`() {
                val function = Func(name = "foo")
                val variable = Variable(name = function.name)
                val call = Call(callee = variable)
                val ast: List<ParserStatement> = listOf(function, ExprStmt(call))
                val symbol = CallableSymbol()

                val symbolTable = SymbolTable(
                    mutableMapOf(function.nodeId to symbol))
                val refResolutions = mapOf(variable.nodeId to function.nodeId)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    VoidType,
                    inference.typeResolutions[call.nodeId])
            }

            @Test
            fun `Variable referring to function structure should return FunctionType`() {
                val returnType = ObjectType(ParserPrimitiveClass.Int.className)
                val function = Func(
                    name = "foo",
                    returnType = returnType)
                val variable = Variable(name = function.name)
                val call = Call(callee = variable)
                val ast: List<ParserStatement> = listOf(function, ExprStmt(call))
                val symbol = CallableSymbol()

                val symbolTable = SymbolTable(
                    mutableMapOf(function.nodeId to symbol))
                val refResolutions = mapOf(variable.nodeId to function.nodeId)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    FunctionType(returnType = returnType),
                    inference.typeResolutions[variable.nodeId])
            }
        }

        @Nested
        inner class ClassContextTests {

            @Test
            fun `Class call should return ObjectType(ClassName)`() {
                val clazz = Class(name = "Foo")
                val variable = Variable(name = clazz.name)
                val call = Call(callee = variable)

                val ast = listOf<ParserStatement>(ExprStmt(call))
                val initSymbol = CallableSymbol()
                val classSymbol = ClassSymbol(
                    signature = ClassSymbol.ClassSignature(
                        name = clazz.name,
                        constructorMethod = initSymbol),
                    hasPrimaryConstructor = false)

                val symbolTable = SymbolTable(mutableMapOf(
                    clazz.nodeId to classSymbol))
                val refResolutions = mapOf(variable.nodeId to clazz.nodeId)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    ObjectType(clazz.name),
                    inference.typeResolutions[call.nodeId])
            }
        }

        @Nested
        inner class VariableContextTests {

            @Test
            fun `Call on variable storing a callable should return callable return type`() {
                val function = Func(
                    name = "foo",
                    returnType = intOT)
                val callVar = Variable(
                    name = "callVar")
                val letVar = Variable(
                    name = "letVar")
                val let = Let(
                    name = "fooFunction",
                    value = letVar,
                    type = AnyType,
                    isMutable = false)
                val call = Call(callVar)

                val callableSymbol = CallableSymbol()
                val letSymbol = VariableSymbol(
                    signature = VariableSymbol.VariableSignature(
                        type = AnyType,
                        isMutable = false))

                val ast = listOf(function, let, ExprStmt(call))

                val symbolTable = SymbolTable(mutableMapOf(
                    function.nodeId to callableSymbol,
                    let.nodeId to letSymbol))
                val refResolutions = mapOf(
                    letVar.nodeId to function.nodeId,
                    callVar.nodeId to let.nodeId)

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    intOT,
                    inference.typeResolutions[call.nodeId])
            }

            @Test
            fun `Call on variable storing a non-callable should throw`() {
                val let = Let(
                    name = "foo",
                    type = AnyType,
                    value = Literal(ParserInt(1)),
                    isMutable = false)
                val letVar = Variable(
                    name = "letVar")
                val call = Call(letVar)

                val letSymbol = VariableSymbol(
                    signature = VariableSymbol.VariableSignature(
                        type = AnyType,
                        isMutable = false))

                val ast = listOf(let, ExprStmt(call))

                val symbolTable = SymbolTable(mutableMapOf(
                    let.nodeId to letSymbol))
                val refResolutions = mapOf(
                    letVar.nodeId to let.nodeId)

                assertThrows<DIRUnexpectedExpressionException> {
                    TypeInference(ast, symbolTable, refResolutions)
                        .infer()
                }
            }
        }
    }

    @Nested
    inner class UnaryTests {

        private val symbolTable = SymbolTable()
        private val refResolutions = emptyMap<Int, Int>()

        private val boolNeg = "!"
        private val mathNeg = "-"

        @Test
        fun `Unary operation on Void should throw`() {
            val unary = Unary(
                operator = boolNeg,   // NOTE: arbitrary operator (control flow unreached)
                expr = Literal(ParserVoid))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            assertThrows<DIRUnexpectedVoidTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Unary operation on Unknown should throw`() {
            val unary = Unary(
                operator = boolNeg,   // NOTE: arbitrary operator (control flow unreached)
                expr = Literal(ParserNotAssigned))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            assertThrows<DIRUnexpectedUnknownTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Negative math unary operation on numeric should pass`() {
            val unary = Unary(
                operator = mathNeg,
                expr = Literal(ParserInt(1)))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                ObjectType(ParserPrimitiveClass.Int.className),
                inference.typeResolutions[unary.nodeId])
        }

        @Test
        fun `Negative math unary operation on non-numeric should throw`() {
            val unary = Unary(
                operator = mathNeg,
                expr = Literal(ParserString("Hello, Drift!")))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            assertThrows<DIRUnsupportedOperationException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Negative bool unary operation on boolean should pass`() {
            val unary = Unary(
                operator = boolNeg,
                expr = Literal(ParserBool(true)))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                ObjectType(ParserPrimitiveClass.Bool.className),
                inference.typeResolutions[unary.nodeId])
        }

        @Test
        fun `Negative bool unary operation on non-boolean should throw`() {
            val unary = Unary(
                operator = boolNeg,
                expr = Literal(ParserString("Hello, Drift!")))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            assertThrows<DIRUnsupportedOperationException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Unexpected operator on unary operation should throw`() {
            val unary = Unary(
                operator = "«",
                expr = Literal(ParserString("Hello, Drift!")))

            val ast: List<ParserStatement> = listOf(ExprStmt(unary))

            assertThrows<DIRUnsupportedOperationException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }
    }

    @Nested
    inner class BinaryTests {

        private val add = "+" ; private val sub = "-"
        private val mul = "*" ; private val div = "/"
        private val mod = "%" ; private val lt  = "<"
        private val loe = "<="; private val gt  = ">"
        private val goe = ">="; private val and = "&&"
        private val or  = "||"; private val eq  = "=="
        private val neq = "!="; private val irg = ".."
        private val erg = "..<"

        private val symbolTable = SymbolTable()
        private val refResolutions = emptyMap<Int, Int>()


        @Test
        fun `Binary operation on Void should throw`() {
            val binaryLeftAttempt = Binary(
                operator = add,   // NOTE: arbitrary operator (control flow unreached)
                left = Literal(ParserVoid),
                right = Literal(ParserInt(1)))
            val binaryRightAttempt = Binary(
                operator = add,   // NOTE: arbitrary operator (control flow unreached)
                left = Literal(ParserInt(1)),
                right = Literal(ParserVoid))

            val astLeftAttempt: List<ParserStatement> = listOf(ExprStmt(binaryLeftAttempt))
            val astRightAttempt: List<ParserStatement> = listOf(ExprStmt(binaryRightAttempt))

            assertThrows<DIRUnexpectedVoidTypeException>(
                "Void as left value in a binary expression should throw") {

                TypeInference(astLeftAttempt, symbolTable, refResolutions)
                    .infer()
            }

            assertThrows<DIRUnexpectedVoidTypeException>(
                "Void as right value in a binary expression should throw") {

                TypeInference(astRightAttempt, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Binary operation on Unknown should throw`() {
            val binaryLeftAttempt = Binary(
                operator = add,   // NOTE: arbitrary operator (control flow unreached)
                left = Literal(ParserNotAssigned),
                right = Literal(ParserInt(1)))
            val binaryRightAttempt = Binary(
                operator = add,   // NOTE: arbitrary operator (control flow unreached)
                left = Literal(ParserInt(1)),
                right = Literal(ParserNotAssigned))

            val astLeftAttempt: List<ParserStatement> = listOf(ExprStmt(binaryLeftAttempt))
            val astRightAttempt: List<ParserStatement> = listOf(ExprStmt(binaryRightAttempt))

            assertThrows<DIRUnexpectedUnknownTypeException>(
                "Unknown as left value in a binary expression should throw") {

                TypeInference(astLeftAttempt, symbolTable, refResolutions)
                    .infer()
            }

            assertThrows<DIRUnexpectedUnknownTypeException>(
                "Unknown as right value in a binary expression should throw") {

                TypeInference(astRightAttempt, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Binary operation with unexisting operator should throw`() {
            val unexistingOperator = "™"
            val binary = Binary(
                left = intSample,
                right = intSample,
                operator = unexistingOperator)

            val ast = listOf<ParserStatement>(ExprStmt(binary))

            assertThrows<DIRUnsupportedOperationException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }


        @Nested
        inner class AdditionTests {

            @Test
            fun `Addition operation with string as left operand should return left type`() {
                val binary = Binary(
                    left = stringSample,
                    right = intSample,
                    operator = add)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    ObjectType(ParserPrimitiveClass.String.className),
                    inference.typeResolutions[binary.nodeId])
            }

            @Test
            fun `Addition operation with numeric as operands should return the promoted numeric type`() {
                val binary = Binary(
                    left = intSample,
                    right = int64Sample,
                    operator = add) // NOTE: type should be promoted to Int64

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    int64OT,
                    inference.typeResolutions[binary.nodeId])
            }

            @Test
            fun `Addition operation with unsupported type as an operand should throw`() {
                val binary = Binary(
                    left = Literal(ParserBool(true)),
                    right = Literal(ParserInt64(2L)),
                    operator = add)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                assertThrows<DIRUnsupportedOperationException> {
                    TypeInference(ast, symbolTable, refResolutions)
                        .infer()
                }
            }
        }

        @Nested
        inner class SubMulDivModTests {
            
            @Test
            fun `Operation with numeric type as operands should return promoted numeric type`() {
                fun sample(op: String) = Binary(
                    left = intSample,
                    right = int64Sample,
                    operator = op)
                
                val subBinary = sample(sub)
                val mulBinary = sample(mul)
                val divBinary = sample(div)
                val modBinary = sample(mod)
                
                val subAst = listOf<ParserStatement>(ExprStmt(subBinary))
                val mulAst = listOf<ParserStatement>(ExprStmt(mulBinary))
                val divAst = listOf<ParserStatement>(ExprStmt(divBinary))
                val modAst = listOf<ParserStatement>(ExprStmt(modBinary))
                
                val subInf = TypeInference(subAst, symbolTable, refResolutions)
                    .infer()

                val mulInf = TypeInference(mulAst, symbolTable, refResolutions)
                    .infer()

                val divInf = TypeInference(divAst, symbolTable, refResolutions)
                    .infer()

                val modInf = TypeInference(modAst, symbolTable, refResolutions)
                    .infer()
                
                assertEquals(
                    int64OT,
                    subInf.typeResolutions[subBinary.nodeId]) {
                    
                    "Substraction between two numerics should return promoted numeric type"
                }

                assertEquals(
                    int64OT,
                    mulInf.typeResolutions[mulBinary.nodeId]) {

                    "Multiplication between two numerics should return promoted numeric type"
                }

                assertEquals(
                    int64OT,
                    divInf.typeResolutions[divBinary.nodeId]) {

                    "Division between two numerics should return promoted numeric type"
                }

                assertEquals(
                    int64OT,
                    modInf.typeResolutions[modBinary.nodeId]) {

                    "Modulo between two numerics should return promoted numeric type"
                }
            }

            @Test
            fun `Operation with unsupported types as operands should throw`() {
                fun sample(op: String) = Binary(
                    left = stringSample,
                    right = int64Sample,
                    operator = op)

                val subBinary = sample(sub)
                val mulBinary = sample(mul)
                val divBinary = sample(div)
                val modBinary = sample(mod)

                val subAst = listOf<ParserStatement>(ExprStmt(subBinary))
                val mulAst = listOf<ParserStatement>(ExprStmt(mulBinary))
                val divAst = listOf<ParserStatement>(ExprStmt(divBinary))
                val modAst = listOf<ParserStatement>(ExprStmt(modBinary))

                assertThrows<DIRUnsupportedOperationException>(
                    "Substraction with unsupported type should throw") {

                    TypeInference(subAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    "Multiplication with unsupported type should throw") {

                    TypeInference(mulAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    "Division with unsupported type should throw") {

                    TypeInference(divAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    "Modulo with unsupported type should throw") {

                    TypeInference(modAst, symbolTable, refResolutions)
                        .infer()
                }
            }
        }

        @Nested
        inner class NumericComparisonTests {

            @Test
            fun `Comparison with numeric type as operands should return Bool type`() {
                fun sample(op: String) = Binary(
                    left = intSample,
                    right = int64Sample,
                    operator = op)

                val errorMsg = "Numeric comparison between two numerics should return boolean type"

                val ltBinary = sample(lt)
                val loeBinary = sample(loe)
                val gtBinary = sample(gt)
                val goeBinary = sample(goe)

                val ltAst = listOf<ParserStatement>(ExprStmt(ltBinary))
                val loeAst = listOf<ParserStatement>(ExprStmt(loeBinary))
                val gtAst = listOf<ParserStatement>(ExprStmt(gtBinary))
                val goeAst = listOf<ParserStatement>(ExprStmt(goeBinary))

                val ltInf = TypeInference(ltAst, symbolTable, refResolutions)
                    .infer()

                val loeInf = TypeInference(loeAst, symbolTable, refResolutions)
                    .infer()

                val gtInf = TypeInference(gtAst, symbolTable, refResolutions)
                    .infer()

                val goeInf = TypeInference(goeAst, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    boolOT,
                    ltInf.typeResolutions[ltBinary.nodeId]) {

                    errorMsg
                }

                assertEquals(
                    boolOT,
                    loeInf.typeResolutions[loeBinary.nodeId]) {

                    errorMsg
                }

                assertEquals(
                    boolOT,
                    gtInf.typeResolutions[gtBinary.nodeId]) {

                    errorMsg
                }

                assertEquals(
                    boolOT,
                    goeInf.typeResolutions[goeBinary.nodeId]) {

                    errorMsg
                }
            }

            @Test
            fun `Comparison with unsupported types as operands should throw`() {
                fun sample(op: String) = Binary(
                    left = stringSample,
                    right = int64Sample,
                    operator = op)

                val errorMsg = "Numeric comparison between two unsupported types should throw"

                val ltBinary = sample(lt)
                val loeBinary = sample(loe)
                val gtBinary = sample(gt)
                val goeBinary = sample(goe)

                val ltAst = listOf<ParserStatement>(ExprStmt(ltBinary))
                val loeAst = listOf<ParserStatement>(ExprStmt(loeBinary))
                val gtAst = listOf<ParserStatement>(ExprStmt(gtBinary))
                val goeAst = listOf<ParserStatement>(ExprStmt(goeBinary))

                assertThrows<DIRUnsupportedOperationException>(
                    errorMsg) {

                    TypeInference(ltAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    errorMsg) {

                    TypeInference(loeAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    errorMsg) {

                    TypeInference(gtAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    errorMsg) {

                    TypeInference(goeAst, symbolTable, refResolutions)
                        .infer()
                }
            }
        }

        @Nested
        inner class BooleanComparisonTests {

            @Test
            fun `Comparison with boolean operands should return Bool type`() {
                fun sample(op: String) = Binary(
                    left = boolSample,
                    right = boolSample,
                    operator = op)

                val andBinary = sample(and)
                val orBinary = sample(or)

                val andAst = listOf<ParserStatement>(ExprStmt(andBinary))
                val orAst = listOf<ParserStatement>(ExprStmt(orBinary))

                val andInf = TypeInference(andAst, symbolTable, refResolutions)
                    .infer()

                val orInf = TypeInference(orAst, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    boolOT,
                    andInf.typeResolutions[andBinary.nodeId]) {

                    "AND comparison with boolean operands should return boolean type"
                }

                assertEquals(
                    boolOT,
                    orInf.typeResolutions[orBinary.nodeId]) {

                    "OR comparison with boolean operands should return boolean type"
                }
            }

            @Test
            fun `Comparison with unsupported type as operand should throw`() {
                fun sample(op: String) = Binary(
                    left = stringSample,
                    right = int64Sample,
                    operator = op)

                val andBinary = sample(and)
                val orBinary = sample(or)

                val andAst = listOf<ParserStatement>(ExprStmt(andBinary))
                val orAst = listOf<ParserStatement>(ExprStmt(orBinary))

                assertThrows<DIRUnsupportedOperationException>(
                    "AND with unsupported type as operand should throw") {

                    TypeInference(andAst, symbolTable, refResolutions)
                        .infer()
                }

                assertThrows<DIRUnsupportedOperationException>(
                    "OR with unsupported type as operand should throw") {

                    TypeInference(orAst, symbolTable, refResolutions)
                        .infer()
                }
            }
        }

        @Nested
        inner class EqComparisonTests {

            @Test
            fun `Operation with any operand type should return Bool type`() {
                fun sample(op: String) = Binary(
                    left = boolSample,
                    right = int64Sample,
                    operator = op)

                val eqBinary = sample(eq)
                val neqBinary = sample(neq)

                val eqAst = listOf<ParserStatement>(ExprStmt(eqBinary))
                val neqAst = listOf<ParserStatement>(ExprStmt(neqBinary))

                val eqInference = TypeInference(eqAst, symbolTable, refResolutions)
                    .infer()

                val neqInference = TypeInference(neqAst, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    boolOT,
                    eqInference.typeResolutions[eqBinary.nodeId]) {

                    "EQ comparison should return boolean type"
                }

                assertEquals(
                    boolOT,
                    neqInference.typeResolutions[neqBinary.nodeId]) {

                    "NEQ comparison should return boolean type"
                }
            }
        }

        @Nested
        inner class InclusiveRangeTests {

            private fun rangeType(leftType: ParserType, rightType: ParserType) =
                ObjectType("InclusiveRange", mapOf(
                    "limitType" to SingleType(
                        promoteNumericTypes(leftType, rightType))))

            @Test
            fun `Range with numeric operands should return ObjectType(InclusiveRange) of promoted numeric type`() {
                val leftType = ObjectType(ParserPrimitiveClass.Int64.className)
                val rightType = ObjectType(ParserPrimitiveClass.Int.className)

                val binary = Binary(
                    left = int64Sample,
                    right = intSample,
                    operator = irg)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    rangeType(leftType, rightType),
                    inference.typeResolutions[binary.nodeId])
            }

            @Test
            fun `Range with unsupported types as operands should throw`() {
                val binary = Binary(
                    left = stringSample,
                    right = intSample,
                    operator = irg)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                assertThrows<DIRUnsupportedOperationException> {
                    TypeInference(ast, symbolTable, refResolutions)
                        .infer()
                }
            }
        }

        @Nested
        inner class ExclusiveRangeTests {

            private fun rangeType(leftType: ParserType, rightType: ParserType) =
                ObjectType("ExclusiveRange", mapOf(
                    "limitType" to SingleType(
                        promoteNumericTypes(leftType, rightType))))

            @Test
            fun `Range with numeric operands should return ObjectType(ExclusiveRange) of promoted numeric type`() {
                val leftType = ObjectType(ParserPrimitiveClass.Int64.className)
                val rightType = ObjectType(ParserPrimitiveClass.Int.className)

                val binary = Binary(
                    left = int64Sample,
                    right = intSample,
                    operator = erg)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                val inference = TypeInference(ast, symbolTable, refResolutions)
                    .infer()

                assertEquals(
                    rangeType(leftType, rightType),
                    inference.typeResolutions[binary.nodeId])
            }

            @Test
            fun `Range with unsupported types as operands should throw`() {
                val binary = Binary(
                    left = stringSample,
                    right = intSample,
                    operator = erg)

                val ast = listOf<ParserStatement>(ExprStmt(binary))

                assertThrows<DIRUnsupportedOperationException> {
                    TypeInference(ast, symbolTable, refResolutions)
                        .infer()
                }
            }
        }
    }

    @Nested
    inner class ConditionalTests {

        private val symbolTable = SymbolTable()
        private val refResolutions = mapOf<Int, Int>()


        @Test
        fun `Conditional with non-boolean condition should throw`() {
            val conditional = Conditional(
                condition = intSample,  // NOTE: Int as a condition is not supported
                thenBranch = Block.empty())

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Conditional which has same type on both branches should return this type`() {
            val block = Block(listOf(
                Return(intSample)))
            val conditional = Conditional(
                condition = boolSample,
                thenBranch = block,
                elseBranch = block)

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                intOT,
                inference.typeResolutions[conditional.nodeId])
        }

        @Test
        fun `Conditional which has different non-null types on both branches should return UnionType(thenType, elseType)`() {
            val expectedType = UnionType(listOf(intOT, stringOT))
            val thenBlock = Block(listOf(
                Return(intSample)))
            val elseBlock = Block(listOf(
                Return(stringSample)))
            val conditional = Conditional(
                condition = boolSample,
                thenBranch = thenBlock,
                elseBranch = elseBlock)

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                expectedType,
                inference.typeResolutions[conditional.nodeId])
        }

        @Test
        fun `Conditional which has only then branch returning null should return OptionalType(elseType)`() {
            val expectedType = OptionalType(stringOT)
            val thenBlock = Block(listOf(
                Return(Literal(ParserNull))))
            val elseBlock = Block(listOf(
                Return(stringSample)))
            val conditional = Conditional(
                condition = boolSample,
                thenBranch = thenBlock,
                elseBranch = elseBlock)

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                expectedType,
                inference.typeResolutions[conditional.nodeId])
        }

        @Test
        fun `Conditional which has only else branch returning null should return OptionalType(thenType)`() {
            val expectedType = OptionalType(intOT)
            val thenBlock = Block(listOf(
                Return(intSample)))
            val elseBlock = Block(listOf(
                Return(Literal(ParserNull))))
            val conditional = Conditional(
                condition = boolSample,
                thenBranch = thenBlock,
                elseBranch = elseBlock)

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                expectedType,
                inference.typeResolutions[conditional.nodeId])
        }

        @Test
        fun `Conditional which has both branches returning null should return NullType`() {
            val block = Block(listOf(
                Return(Literal(ParserNull))))
            val conditional = Conditional(
                condition = boolSample,
                thenBranch = block,
                elseBranch = block)

            val ast = listOf<ParserStatement>(ExprStmt(conditional))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                NullType,
                inference.typeResolutions[conditional.nodeId])
        }
    }

    @Nested
    inner class AssignTests {

        private val symbolTable = SymbolTable()
        private val refResolutions = mapOf<Int, Int>()


        @Test
        fun `Assign should return value type`() {
            val assign = Assign(
                name = "foo",
                value = stringSample)

            val ast = listOf<ParserStatement>(ExprStmt(assign))

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                stringOT,
                inference.typeResolutions[assign.nodeId])
        }
    }

    @Nested
    inner class GetTests {

        @Test
        fun `Get from non-object should throw`() {
            val receiver = Literal(ParserNull)
            val get = Get(
                receiver = receiver,
                name = "foo")

            val ast = listOf<ParserStatement>(ExprStmt(get))

            val symbolTable = SymbolTable()
            val refResolutions = mapOf<Int, Int>()

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Get field from object should return field type`() {
            val fooField = Let(
                name = "foo",
                type = AnyType,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                fields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    fields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                stringOT,
                inference.typeResolutions[get.nodeId])
        }

        @Test
        fun `Get static field from object should return field type`() {
            val fooField = Let(
                name = "foo",
                type = AnyType,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                staticFields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    staticFields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                stringOT,
                inference.typeResolutions[get.nodeId])
        }

        @Test
        fun `Get method from object should return FunctionType`() {
            val fooField = Func(
                name = "foo")
            val stringClass = Class(
                name = "String",
                methods = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    methods = linkedMapOf(
                        "foo" to CallableSymbol.CallableSignature())),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                FunctionType(),
                inference.typeResolutions[get.nodeId])
        }

        @Test
        fun `Get static method from object should return FunctionType`() {
            val fooField = Func(
                name = "foo")
            val stringClass = Class(
                name = "String",
                staticMethods = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    staticMethods = linkedMapOf(
                        "foo" to CallableSymbol.CallableSignature())),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                FunctionType(),
                inference.typeResolutions[get.nodeId])
        }

        @Test
        fun `Get unexisting member from object should throw`() {
            val stringClass = Class(
                name = "String")
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol()),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            assertThrows<DIRNotDefinedSymbolException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Get unexisting static member from object should throw`() {
            val stringClass = Class(
                name = "String")
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol()),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val get = Get(
                receiver = receiver,
                "foo")

            val ast = listOf(stringClass, ExprStmt(get))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            assertThrows<DIRNotDefinedSymbolException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }
    }

    @Nested
    inner class SetTests {

        @Test
        fun `Set from non-object should throw`() {
            val receiver = Literal(ParserNull)
            val set = Set(
                receiver = receiver,
                name = "foo",
                value = stringSample)

            val ast = listOf<ParserStatement>(ExprStmt(set))

            val symbolTable = SymbolTable()
            val refResolutions = mapOf<Int, Int>()

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Set field from object should return field type`() {
            val fooField = Let(
                name = "foo",
                type = AnyType,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                fields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    fields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = stringSample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                stringOT,
                inference.typeResolutions[set.nodeId])
        }

        @Test
        fun `Set static field from object should return field type`() {
            val fooField = Let(
                name = "foo",
                type = AnyType,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                staticFields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    staticFields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = stringSample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            val inference = TypeInference(ast, symbolTable, refResolutions)
                .infer()

            assertEquals(
                stringOT,
                inference.typeResolutions[set.nodeId])
        }

        @Test
        fun `Set unexisting field from object should throw`() {
            val stringClass = Class(
                name = "String")
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol()),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = stringSample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            assertThrows<DIRNotDefinedSymbolException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Set unexisting static field from object should throw`() {
            val stringClass = Class(
                name = "String")
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol()),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = stringSample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            assertThrows<DIRNotDefinedSymbolException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Set field with not compatible value should throw`() {
            val fooField = Let(
                name = "foo",
                type = stringOT,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                fields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    fields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = stringSample

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = int64Sample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf<Int, Int>()

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }

        @Test
        fun `Set static field with not compatible value should throw`() {
            val fooField = Let(
                name = "foo",
                type = stringOT,
                value = stringSample,
                isMutable = false)
            val stringClass = Class(
                name = "String",
                staticFields = mutableListOf(
                    fooField))
            val stringClassSymbol = ClassSymbol(
                signature = ClassSymbol.ClassSignature(
                    name = "String",
                    constructorMethod = CallableSymbol(),
                    staticFields = linkedMapOf(
                        "foo" to stringOT)),
                hasPrimaryConstructor = false)

            val receiver = Variable(
                name = "String")

            val set = Set(
                receiver = receiver,
                name = "foo",
                value = int64Sample)

            val ast = listOf(stringClass, ExprStmt(set))

            val symbolTable = SymbolTable()
            symbolTable.addClass(
                nodeId = stringClass.nodeId,
                signature = stringClassSymbol.signature,
                hasPrimaryConstructor = false)
            val refResolutions = mapOf(
                receiver.nodeId to stringClass.nodeId)

            assertThrows<DIRUnexpectedTypeException> {
                TypeInference(ast, symbolTable, refResolutions)
                    .infer()
            }
        }
    }


    @Nested
    inner class ForTests {

        val symbolTable = SymbolTable()
        val refResolutions = emptyMap<Int, Int>()


        @Test
        fun `For loop iterable expression type is resolved`() {
            val iterable = intSample
            val forStmt = For(
                iterable = iterable,
                variables = emptyList(),
                body = Block.empty())
            val ast: List<ParserStatement> = listOf(forStmt)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(intOT, inference.typeResolutions[iterable.nodeId])
        }

        @Test
        fun `For loop return type does not propagate to containing function`() {
            val forStmt = For(
                iterable = intSample,
                variables = emptyList(),
                body = Block(listOf(Return(value = intSample))))
            val func = Func(
                name = "foo",
                body = Block(listOf(forStmt)))
            val ast: List<ParserStatement> = listOf(func)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(VoidType, inference.typeResolutions[func.nodeId])
        }
    }


    @Nested
    inner class ClassTests {

        val symbolTable = SymbolTable()
        val refResolutions = emptyMap<Int, Int>()


        @Test
        fun `Class field type is resolved after class inference`() {
            val field = Let(name = "x", type = intOT, value = intSample, isMutable = false)
            val clazz = Class(name = "Foo", fields = mutableListOf(field))
            val ast: List<ParserStatement> = listOf(clazz)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(intOT, inference.typeResolutions[field.nodeId])
        }

        @Test
        fun `Class method return type is resolved after class inference`() {
            val method = Func(name = "get", returnType = intOT)
            val clazz = Class(name = "Foo", methods = mutableListOf(method))
            val ast: List<ParserStatement> = listOf(clazz)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(intOT, inference.typeResolutions[method.nodeId])
        }

        @Test
        fun `Class static field type is resolved after class inference`() {
            val field = Let(name = "count", type = intOT, value = intSample, isMutable = false)
            val clazz = Class(name = "Foo", staticFields = mutableListOf(field))
            val ast: List<ParserStatement> = listOf(clazz)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(intOT, inference.typeResolutions[field.nodeId])
        }

        @Test
        fun `Class static method return type is resolved after class inference`() {
            val method = Func(name = "create", returnType = intOT)
            val clazz = Class(name = "Foo", staticMethods = mutableListOf(method))
            val ast: List<ParserStatement> = listOf(clazz)

            val inference = TypeInference(ast, symbolTable, refResolutions).infer()

            assertEquals(intOT, inference.typeResolutions[method.nodeId])
        }
    }
}