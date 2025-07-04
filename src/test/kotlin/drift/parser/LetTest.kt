package drift.parser

import drift.ast.DrStmt
import drift.ast.eval
import drift.exceptions.DriftParserException
import drift.exceptions.DriftRuntimeException
import drift.runtime.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LetTest {

    private fun parse(code: String): List<DrValue> {
        val statements: List<DrStmt> = Parser(lex(code)).parse()
        val outputs = mutableListOf<DrValue>()
        val env = DrEnv().apply {
            define(
                "print", DrNativeFunction(
                    impl = { args ->
                        outputs.add(args[0].second)
                        DrNull
                    },
                    paramTypes = listOf(AnyType),
                    returnType = NullType
                )
            )
        }

        for (statement in statements) {
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
        val output = parse("""
            let a : String = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
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
        val output = parse("""
            var a : String = 1
            
            print(a)
        """.trimIndent())

        assertEquals(listOf(DrInt(1)), output.map {
            if (it is DrVariable) it.value
            else it
        })
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
}