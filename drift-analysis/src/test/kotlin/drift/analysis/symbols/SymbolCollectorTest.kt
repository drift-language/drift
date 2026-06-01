package drift.analysis.symbols

import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.*
import drift.ast.statements.*
import drift.ast.statements.hooks.UnreturnableHook
import drift.oldruntime.AnyType
import drift.oldruntime.ObjectType
import drift.oldruntime.values.primaries.ParserInt
import drift.oldruntime.values.specials.ParserNotAssigned
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SymbolCollectorTest {

    private fun collect(vararg statements: ParserStatement) =
        SymbolCollector(SymbolTable(), statements.toList()).collect()

    private fun classWithInit(
        name: String,
        fields: List<Let> = emptyList(),
        methods: List<Func> = emptyList()) = Class(
            name = name,
            fields = fields.toMutableList(),
            methods = methods.toMutableList(),
            hooks = mutableListOf(UnreturnableHook(name = "init")))


    @Nested
    inner class VariableTests {

        @Test
        fun `Let should be registered as a variable symbol`() {
            val let = Let(name = "x", type = AnyType, value = Literal(ParserInt(1)), isMutable = false)
            val result = collect(let)
            val nodeId = result.symbolTable.lookupNodeId("x")
            assertNotNull(nodeId)
            assertInstanceOf(VariableSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Let with ObjectType annotation resolves to class when class is defined before`() {
            val clazz = classWithInit("Foo")
            val let = Let(name = "x", type = ObjectType("Foo"), value = Literal(ParserNotAssigned), isMutable = false)
            val result = collect(clazz, let)
            assertEquals(clazz.nodeId, result.resolutions[let.nodeId])
        }

        @Test
        fun `Let with ObjectType annotation is not resolved when class is undefined`() {
            val let = Let(name = "x", type = ObjectType("Unknown"), value = Literal(ParserInt(1)), isMutable = false)
            val result = collect(let)
            assertNull(result.resolutions[let.nodeId])
        }
    }


    @Nested
    inner class FunctionTests {

        @Test
        fun `Function should be registered as a callable symbol`() {
            val func = Func(name = "foo")
            val result = collect(func)
            val nodeId = result.symbolTable.lookupNodeId("foo")
            assertNotNull(nodeId)
            assertInstanceOf(CallableSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Function with required parameter registers it as required in signature`() {
            val param = FunctionParameter(name = "x", type = AnyType)
            val func = Func(name = "foo", parameters = listOf(param))
            val result = collect(func)
            val symbol = result.symbolTable.getSymbol(func.nodeId) as CallableSymbol
            assertTrue(symbol.signature.parameterTypes.single().isRequired)
        }

        @Test
        fun `Function with optional parameter registers it as not required in signature`() {
            val param = FunctionParameter(name = "x", type = AnyType, defaultValue = Literal(ParserInt(0)))
            val func = Func(name = "foo", parameters = listOf(param))
            val result = collect(func)
            val symbol = result.symbolTable.getSymbol(func.nodeId) as CallableSymbol
            assertFalse(symbol.signature.parameterTypes.single().isRequired)
        }
    }


    @Nested
    inner class ClassTests {

        @Test
        fun `Class should be registered as a class symbol`() {
            val clazz = classWithInit("Foo")
            val result = collect(clazz)
            val nodeId = result.symbolTable.lookupNodeId("Foo")
            assertNotNull(nodeId)
            assertInstanceOf(ClassSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Class fields are included in the class signature`() {
            val field = Let(name = "x", type = ObjectType("Int"), value = Literal(ParserNotAssigned), isMutable = false)
            val clazz = classWithInit("Foo", fields = listOf(field))
            val result = collect(clazz)
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertEquals(ObjectType("Int"), symbol.signature.fields["x"])
        }

        @Test
        fun `Class methods are included in the class signature`() {
            val method = Func(name = "greet")
            val clazz = classWithInit("Foo", methods = listOf(method))
            val result = collect(clazz)
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertTrue(symbol.signature.methods.containsKey("greet"))
        }

        @Test
        fun `Class static fields are included in the class signature`() {
            val field = Let(name = "count", type = ObjectType("Int"), value = Literal(ParserNotAssigned), isMutable = false)
            val clazz = Class(
                name = "Foo",
                staticFields = mutableListOf(field),
                hooks = mutableListOf(UnreturnableHook(name = "init")))
            val result = collect(clazz)
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertEquals(ObjectType("Int"), symbol.signature.staticFields["count"])
        }

        @Test
        fun `Class static methods are included in the class signature`() {
            val method = Func(name = "create")
            val clazz = Class(
                name = "Foo",
                staticMethods = mutableListOf(method),
                hooks = mutableListOf(UnreturnableHook(name = "init")))
            val result = collect(clazz)
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertTrue(symbol.signature.staticMethods.containsKey("create"))
        }
    }


    @Nested
    inner class ReferenceResolutionTests {

        @Test
        fun `Variable reference resolves to definition nodeId`() {
            val let = Let(name = "x", type = AnyType, value = Literal(ParserInt(1)), isMutable = false)
            val ref = Variable("x")
            val result = collect(let, ExprStmt(ref))
            assertEquals(let.nodeId, result.resolutions[ref.nodeId])
        }

        @Test
        fun `Assign resolves to definition nodeId`() {
            val let = Let(name = "x", type = AnyType, value = Literal(ParserInt(1)), isMutable = true)
            val assign = Assign(name = "x", value = Literal(ParserInt(2)))
            val result = collect(let, ExprStmt(assign))
            assertEquals(let.nodeId, result.resolutions[assign.nodeId])
        }

        @Test
        fun `Reference to undefined name has no resolution`() {
            val ref = Variable("undefined")
            val result = collect(ExprStmt(ref))
            assertNull(result.resolutions[ref.nodeId])
        }
    }


    @Nested
    inner class LambdaTests {

        @Test
        fun `Lambda captures outer variable in closure`() {
            val outer = Let(name = "x", type = AnyType, value = Literal(ParserInt(1)), isMutable = false)
            val lambda = Lambda(body = Block(listOf(ExprStmt(Variable("x")))))
            val result = collect(outer, ExprStmt(lambda))
            assertEquals(outer.nodeId, result.lambdaClosures[lambda.nodeId]?.get("x"))
        }

        @Test
        fun `Lambda parameters are not captured as closure variables`() {
            val param = FunctionParameter(name = "x", type = AnyType)
            val lambda = Lambda(
                parameters = listOf(param),
                body = Block(listOf(ExprStmt(Variable("x")))))
            val result = collect(ExprStmt(lambda))
            assertFalse(result.lambdaClosures[lambda.nodeId]?.containsKey("x") == true)
        }
    }
}