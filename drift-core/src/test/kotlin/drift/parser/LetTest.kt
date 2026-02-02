package drift.parser

import drift.parser.exceptions.DPExpectedNewlineBetweenTopLevelStatementsException
import drift.parser.exceptions.DPMissingExpectedTokenException
import drift.parser.exceptions.DPUnallowedVariableInjectionPrefixUsageException
import drift.runtime.exceptions.DRCannotUseVoidAsValueException
import drift.runtime.exceptions.DRUnassignableException
import drift.utils.evalProgram
import drift.utils.evalWithOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class LetTest {

    @Test
    fun `Declare immutable variable without typing`() {
        val output = evalWithOutputs("""
            let a = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare mutable variable without typing`() {
        val output = evalWithOutputs("""
            var a = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare immutable variable with good type`() {
        val output = evalWithOutputs("""
            let a : Int = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare immutable variable with wrong type`() {
        assertThrows<DRUnassignableException> {
            evalWithOutputs("""
                let a : String = 1
                
                test(a)
            """.trimIndent())
        }
    }

    @Test
    fun `Declare immutable variable with union type`() {
        val output = evalWithOutputs("""
            let a : String|Int = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare immutable variable with nullable type`() {
        val output = evalWithOutputs("""
            let a : Int? = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare mutable variable with good type`() {
        val output = evalWithOutputs("""
            var a : Int = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare mutable variable with wrong type`() {
        assertThrows<DRUnassignableException> {
            evalWithOutputs("""
                var a : String = 1
                
                test(a)
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with union type`() {
        val output = evalWithOutputs("""
            var a : String|Int = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare mutable variable with nullable type`() {
        val output = evalWithOutputs("""
            var a : Int? = 1
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare immutable variable with ternary`() {
        val output = evalWithOutputs("""
            var a = true ? 1 : 0
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare mutable variable with ternary`() {
        val output = evalWithOutputs("""
            var a = true ? 1 : 0
            
            test(a)
        """.trimIndent())

        assertEquals(listOf("1"), output)
    }

    @Test
    fun `Declare immutable variable with invalid name`() {
        assertThrows<DPMissingExpectedTokenException> {
            evalWithOutputs("""
                let 55 = 0
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with invalid name`() {
        assertThrows<DPMissingExpectedTokenException> {
            evalWithOutputs("""
                var 55 = 0
            """.trimIndent())
        }
    }

    @Test
    fun `Declare using union int & class types`() {
        assertDoesNotThrow {
            evalWithOutputs("""
                class User(name: String)
                let u: User|Int = User(name = "Bob")
            """.trimIndent())
        }
    }

    @Test
    fun `Declare immutable variable with void value must throw`() {
        assertThrows<DRCannotUseVoidAsValueException> {
            evalWithOutputs("""
                fun foo {}
                
                let a = foo()
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with void value must throw`() {
        assertThrows<DRCannotUseVoidAsValueException> {
            evalWithOutputs("""
                fun foo {}
                
                var a = foo()
            """.trimIndent())
        }
    }

    @Test
    fun `Declare immutable variable with '$' prefix (reserved) must throw`() {
        assertThrows<DPUnallowedVariableInjectionPrefixUsageException> {
            evalProgram("""
                let ${'$'}a = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Declare mutable variable with '$' prefix (reserved) must throw`() {
        assertThrows<DPUnallowedVariableInjectionPrefixUsageException> {
            evalProgram("""
                var ${'$'}a = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Use immutable variable declaration as value must throw`() {
        assertThrows<DPExpectedNewlineBetweenTopLevelStatementsException> {
            evalProgram("""
                let a = let b = 1
            """.trimIndent())
        }
    }

    @Test
    fun `Use mutable variable declaration as value must throw`() {
        assertThrows<DPExpectedNewlineBetweenTopLevelStatementsException> {
            evalProgram("""
                let a = var b = 1
            """.trimIndent())
        }
    }
}