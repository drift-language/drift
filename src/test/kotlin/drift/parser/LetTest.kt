package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.checkers.SymbolCollector
import drift.checkers.TypeChecker
import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import drift.runtime.values.callables.DrNativeFunction
import drift.runtime.values.primaries.DrInt
import drift.runtime.values.specials.DrNull
import drift.runtime.values.variables.DrVariable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LetTest {

    private fun parse(code: String): List<DrValue> {
        val ast: List<DrStmt> = Parser(lex(code)).parse()
        val outputs = mutableListOf<DrValue>()
        val env = DrEnv().apply {
            define(
                "print", DrNativeFunction(
                    impl = { _, args ->
                        outputs.add(args[0].second)
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }

        SymbolCollector(env).collect(ast)
        TypeChecker(env).check(ast)

        for (statement in ast) {
            statement.eval(env)
        }

        return outputs
    }

    @Test
    fun `Declare immutable variable without typing`() {
        val output = parse("""
            let a = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare mutable variable without typing`() {
        val output = parse("""
            var a = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare immutable variable with good type`() {
        val output = parse("""
            let a : Int = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare immutable variable with wrong type`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                let a : String = 1
                
                print(a)
            """.trimIndent())
        }
    }

    @Test
    fun `Declare immutable variable with union type`() {
        val output = parse("""
            let a : String|Int = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare immutable variable with nullable type`() {
        val output = parse("""
            let a : Int? = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare mutable variable with good type`() {
        val output = parse("""
            var a : Int = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare mutable variable with wrong type`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                var a : String = 1
                
                print(a)
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with union type`() {
        val output = parse("""
            var a : String|Int = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare mutable variable with nullable type`() {
        val output = parse("""
            var a : Int? = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare immutable variable with ternary`() {
        val output = parse("""
            var a = true ? 1 : 0
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare mutable variable with ternary`() {
        val output = parse("""
            var a = true ? 1 : 0
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
    }

    @Test
    fun `Declare immutable variable with invalid name`() {
        assertThrows<DriftParserException> {
            parse("""
                let 55 = 0
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with invalid name`() {
        assertThrows<DriftParserException> {
            parse("""
                var 55 = 0
            """.trimIndent())
        }
    }

    @Test
    fun `Declare using union int & class types`() {
        assertDoesNotThrow {
            parse("""
                class User(name: String)
                let u: User|Int = User("Bob")
            """.trimIndent())
        }
    }

    @Test
    fun `Declare immutable variable with void value must throw`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                fun test {}
                
                let a = test()
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with void value must throw`() {
        assertThrows<DriftRuntimeException> {
            parse("""
                fun test {}
                
                var a = test()
            """.trimIndent())
        }
    }
}