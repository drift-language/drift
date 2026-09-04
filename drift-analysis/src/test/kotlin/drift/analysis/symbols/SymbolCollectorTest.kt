package drift.analysis.symbols

import drift.ast.bindings.FunctionParameter
import drift.ast.expressions.*
import drift.ast.statements.*
import drift.ast.statements.hooks.UnreturnableHook
import drift.types.AnyType
import drift.types.ObjectType
import drift.types.UnresolvedObjectType
import drift.types.resolve
import drift.values.ParserPrimitiveClass
import drift.values.primaries.IntValue
import language.InjectedVariableUtils.injectedThis
import language.ModuleReference
import language.Namespace
import language.QualifiedName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SymbolCollectorTest {

    private fun collect(namespace: String = "test", vararg statements: ParserStatement) =
        SymbolCollector(Namespace(namespace), SymbolTable(), statements.toList()).collect()

    private fun qualifiedName(namespace: String, simpleName: String) =
        QualifiedName(module = ModuleReference.unresolved, namespace = Namespace(namespace), simpleName = simpleName).qualifiedName

    private fun intLet(name: String, isMutable: Boolean = false) =
        Let(name = name, type = AnyType, value = Literal(IntValue(1)), isMutable = isMutable)

    private fun classWithInit(
        name: String,
        fields: List<Let> = emptyList(),
        methods: List<Func> = emptyList(),
        staticFields: List<Let> = emptyList(),
        staticMethods: List<Func> = emptyList()) = Class(
            name = name,
            fields = fields.toMutableList(),
            methods = methods.toMutableList(),
            staticFields = staticFields.toMutableList(),
            staticMethods = staticMethods.toMutableList(),
            hooks = mutableListOf(UnreturnableHook(name = "init")))


    @Nested
    inner class ReferenceTests {

        @Test
        fun `Let should be registered as a variable symbol`() {
            // Given
            val let = intLet("x")

            // When
            val result = collect(statements = arrayOf(let))

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName("test", "x"))
            assertNotNull(nodeId)
            assertInstanceOf(VariableSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Let with ObjectType annotation resolves to class when class is defined before`() {
            // Given
            val clazz = classWithInit("Foo")
            val let = Let(name = "x", type = UnresolvedObjectType("Foo"), isMutable = false)

            // When
            val result = collect(statements = arrayOf(clazz, let))

            // Then
            assertEquals(clazz.nodeId, result.resolutions[let.nodeId]!!)
        }

        @Test
        fun `Let with ObjectType annotation is not resolved when class is undefined`() {
            // Given
            val let = Let(name = "x", type = UnresolvedObjectType("Unknown"), value = Literal(IntValue(1)), isMutable = false)

            // When
            val result = collect(statements = arrayOf(let))

            // Then
            assertNull(result.resolutions[let.nodeId])
        }
    }


    @Nested
    inner class FunctionTests {

        @Test
        fun `Function should be registered as a callable symbol`() {
            // Given
            val func = Func(name = "foo")

            // When
            val result = collect(statements = arrayOf(func))

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName("test", "foo"))
            assertNotNull(nodeId)
            assertInstanceOf(CallableSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Function with required parameter registers it as required in signature`() {
            // Given
            val param = FunctionParameter(name = "x", type = AnyType)
            val func = Func(name = "foo", parameters = listOf(param))

            // When
            val result = collect(statements = arrayOf(func))

            // Then
            val symbol = result.symbolTable.getSymbol(func.nodeId) as CallableSymbol
            assertTrue(symbol.signature.parameterTypes.single().isRequired)
        }

        @Test
        fun `Function with optional parameter registers it as not required in signature`() {
            // Given
            val param = FunctionParameter(name = "x", type = AnyType, defaultValue = Literal(IntValue(0)))
            val func = Func(name = "foo", parameters = listOf(param))

            // When
            val result = collect(statements = arrayOf(func))

            // Then
            val symbol = result.symbolTable.getSymbol(func.nodeId) as CallableSymbol
            assertFalse(symbol.signature.parameterTypes.single().isRequired)
        }

        @Test
        fun `Function captures outer variable in closure`() {
            // Given
            val outer = intLet("x")
            val func = Func(name = "foo", body = Block(listOf(ExprStmt(Reference("x")))))

            // When
            val result = collect(statements = arrayOf(outer, func))

            // Then
            assertEquals(outer.nodeId, result.closures[func.nodeId]?.get("x")!!)
        }

        @Test
        fun `Function parameters are not captured as closure variables`() {
            // Given
            val param = FunctionParameter(name = "x", type = AnyType)
            val func = Func(
                name = "foo",
                parameters = listOf(param),
                body = Block(listOf(ExprStmt(Reference("x")))))

            // When
            val result = collect(statements = arrayOf(func))

            // Then
            assertFalse(result.closures[func.nodeId]?.containsKey("x") == true)
        }

        @Test
        fun `Nested function captures enclosing function's local variable`() {
            // Given
            val innerFunc = Func(name = "inner", body = Block(listOf(ExprStmt(Reference("y")))))
            val outerLet = intLet("y")
            val outerFunc = Func(name = "outer", body = Block(listOf(outerLet, innerFunc)))

            // When
            val result = collect(statements = arrayOf(outerFunc))

            // Then
            assertEquals(outerLet.nodeId, result.closures[innerFunc.nodeId]?.get("y")!!)
        }

        @Test
        fun `Method does not capture injected this as a closure variable`() {
            // Given
            val method = Func(name = "greet", body = Block(listOf(ExprStmt(Reference(injectedThis())))))
            val clazz = classWithInit("Foo", methods = listOf(method))

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            assertFalse(result.closures[method.nodeId]?.containsKey(injectedThis()) == true)
        }
    }


    @Nested
    inner class ClassTests {

        @Test
        fun `Class should be registered as a class symbol`() {
            // Given
            val clazz = classWithInit("Foo")

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName("test", "Foo"))
            assertNotNull(nodeId)
            assertInstanceOf(ClassSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `Class fields are included in the class signature`() {
            // Given
            val field = Let(name = "x", type = UnresolvedObjectType("Int"), isMutable = false)
            val clazz = classWithInit("Foo", fields = listOf(field))

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertEquals(ObjectType(ParserPrimitiveClass.Int), symbol.signature.fields["x"])
        }

        @Test
        fun `Class methods are included in the class signature`() {
            // Given
            val method = Func(name = "greet")
            val clazz = classWithInit("Foo", methods = listOf(method))

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertTrue(symbol.signature.methods.containsKey("greet"))
        }

        @Test
        fun `Class static fields are included in the class signature`() {
            // Given
            val field = Let(name = "count", type = UnresolvedObjectType("Int"), isMutable = false)
            val clazz = classWithInit("Foo", staticFields = listOf(field))

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertEquals(ObjectType(ParserPrimitiveClass.Int), symbol.signature.staticFields["count"])
        }

        @Test
        fun `Class static methods are included in the class signature`() {
            // Given
            val method = Func(name = "create")
            val clazz = classWithInit("Foo", staticMethods = listOf(method))

            // When
            val result = collect(statements = arrayOf(clazz))

            // Then
            val symbol = result.symbolTable.getSymbol(clazz.nodeId) as ClassSymbol
            assertTrue(symbol.signature.staticMethods.containsKey("create"))
        }
    }


    @Nested
    inner class ReferenceResolutionTests {

        @Test
        fun `Variable reference resolves to definition nodeId`() {
            // Given
            val let = intLet("x")
            val ref = Reference("x")

            // When
            val result = collect(statements = arrayOf(let, ExprStmt(ref)))

            // Then
            assertEquals(let.nodeId, result.resolutions[ref.nodeId]!!)
        }

        @Test
        fun `Assign resolves to definition nodeId`() {
            // Given
            val let = intLet("x", isMutable = true)
            val assign = Assign(name = "x", value = Literal(IntValue(2)))

            // When
            val result = collect(statements = arrayOf(let, ExprStmt(assign)))

            // Then
            assertEquals(let.nodeId, result.resolutions[assign.nodeId]!!)
        }

        @Test
        fun `Reference to undefined name has no resolution`() {
            // Given
            val ref = Reference("undefined")

            // When
            val result = collect(statements = arrayOf(ExprStmt(ref)))

            // Then
            assertNull(result.resolutions[ref.nodeId])
        }
    }


    @Nested
    inner class LambdaTests {

        @Test
        fun `Lambda captures outer variable in closure`() {
            // Given
            val outer = intLet("x")
            val lambda = Lambda(body = Block(listOf(ExprStmt(Reference("x")))))

            // When
            val result = collect(statements = arrayOf(outer, ExprStmt(lambda)))

            // Then
            assertEquals(outer.nodeId, result.closures[lambda.nodeId]?.get("x")!!)
        }

        @Test
        fun `Lambda parameters are not captured as closure variables`() {
            // Given
            val param = FunctionParameter(name = "x", type = AnyType)
            val lambda = Lambda(
                parameters = listOf(param),
                body = Block(listOf(ExprStmt(Reference("x")))))

            // When
            val result = collect(statements = arrayOf(ExprStmt(lambda)))

            // Then
            assertFalse(result.closures[lambda.nodeId]?.containsKey("x") == true)
        }
    }


    @Nested
    inner class ImportTests {

        private val importedNamespace = "test/users"
        private val currentNamespace = "main"

        private fun myValueLet() = Let(name = "myValue", type = AnyType, isMutable = false)

        private fun tableWithSymbol(let: Let): SymbolTable {
            val st = SymbolTable()
            st.addVariable(
                nodeId = let.nodeId,
                name = let.name,
                signature = VariableSymbol.VariableSignature(
                    type = let.type.resolve(ModuleReference.unresolved, Namespace(importedNamespace)),
                    isMutable = let.isMutable,
                    scopeType = Symbol.TopLevelScope(Namespace(importedNamespace))))
            return st
        }

        private fun usersImport(wildcard: Boolean = false, parts: List<ImportPart>? = null) =
            Import(
                namespace = importedNamespace,
                steps = listOf("test", "users"),
                wildcard = wildcard,
                parts = parts)

        private fun collectImport(import: Import, symbolTable: SymbolTable) =
            SymbolCollector(Namespace(currentNamespace), symbolTable, listOf(import)).collect()


        @Test
        fun `accessor import creates ModuleSymbol bound under current namespace`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport()

            // When
            val result = collectImport(import, st)

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "users"))
            assertNotNull(nodeId)
            assertInstanceOf(ModuleSymbol::class.java, result.symbolTable.getSymbol(nodeId!!))
        }

        @Test
        fun `accessor import ModuleSymbol contains imported symbols by simple name`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport()

            // When
            val result = collectImport(import, st)

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "users"))!!
            val module = result.symbolTable.getSymbol(nodeId) as ModuleSymbol
            assertTrue(module.signature.symbols.containsKey("myValue"))
            assertEquals(myValue.nodeId, module.signature.symbols["myValue"]!!)
        }

        @Test
        fun `selective import without alias binds symbol under current namespace`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport(parts = listOf(ImportPart(source = "myValue")))

            // When
            val result = collectImport(import, st)

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "myValue"))
            assertNotNull(nodeId)
            assertEquals(myValue.nodeId, nodeId!!)
        }

        @Test
        fun `selective import with alias binds symbol under alias in current namespace`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport(parts = listOf(ImportPart(source = "myValue", alias = "mv")))

            // When
            val result = collectImport(import, st)

            // Then
            val aliasNodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "mv"))
            assertNotNull(aliasNodeId)
            assertEquals(myValue.nodeId, aliasNodeId!!)
        }

        @Test
        fun `wildcard import binds all symbols under current namespace`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport(wildcard = true)

            // When
            val result = collectImport(import, st)

            // Then
            val nodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "myValue"))
            assertNotNull(nodeId)
            assertEquals(myValue.nodeId, nodeId!!)
        }

        @Test
        fun `wildcard import with alias renames symbol under current namespace`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport(
                wildcard = true,
                parts = listOf(ImportPart(source = "myValue", alias = "mv")))

            // When
            val result = collectImport(import, st)

            // Then
            val aliasNodeId = result.symbolTable.lookupNodeId(qualifiedName(currentNamespace, "mv"))
            assertNotNull(aliasNodeId)
            assertEquals(myValue.nodeId, aliasNodeId!!)
        }

        @Test
        fun `selective import of undefined symbol should throw`() {
            // Given
            val import = usersImport(parts = listOf(ImportPart(source = "nonExistent")))

            // When / Then
            assertThrows<IllegalStateException> {
                SymbolCollector(Namespace(currentNamespace), SymbolTable(), listOf(import)).collect()
            }
        }

        @Test
        fun `duplicate import of same namespace is silently ignored`() {
            // Given
            val myValue = myValueLet()
            val st = tableWithSymbol(myValue)
            val import = usersImport(parts = listOf(ImportPart(source = "myValue")))

            // When
            val result = SymbolCollector(Namespace(currentNamespace), st, listOf(import, import)).collect()

            // Then
            val bindings = result.symbolTable
                .getBindingsByNamespace(currentNamespace)
                .filter { it.value == myValue.nodeId }
            assertEquals(1, bindings.size)
        }
    }
}
