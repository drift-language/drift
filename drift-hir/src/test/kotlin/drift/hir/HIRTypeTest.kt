/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/

package drift.hir

import kotlin.test.*

class HIRTypeTest {

    @Test
    fun `create primitive int type`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        assertNotNull(intType)
        assertEquals(PrimitiveKind.INT, intType.kind)
    }

    @Test
    fun `create primitive string type`() {
        val stringType = HIRPrimitiveType(PrimitiveKind.STRING)
        assertEquals(PrimitiveKind.STRING, stringType.kind)
    }

    @Test
    fun `create primitive bool type`() {
        val boolType = HIRPrimitiveType(PrimitiveKind.BOOL)
        assertEquals(PrimitiveKind.BOOL, boolType.kind)
    }

    @Test
    fun `create void type`() {
        val voidType = HIRPrimitiveType(PrimitiveKind.VOID)
        assertEquals(PrimitiveKind.VOID, voidType.kind)
    }

    @Test
    fun `create null type`() {
        val nullType = HIRPrimitiveType(PrimitiveKind.NULL)
        assertEquals(PrimitiveKind.NULL, nullType.kind)
    }

    @Test
    fun `create class type`() {
        val userType = HIRClassType("User")
        assertEquals("User", userType.className)
        assertEquals(emptyMap(), userType.typeArguments)
    }

    @Test
    fun `create generic class type`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        val listIntType = HIRClassType("List", mapOf("elementType" to intType))
        assertEquals("List", listIntType.className)
        assertEquals(1, listIntType.typeArguments.size)
        assertTrue(listIntType.typeArguments.containsKey("elementType"))
    }

    @Test
    fun `create optional type`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        val optionalInt = HIROptionalType(intType)
        assertEquals(intType, optionalInt.innerType)
    }

    @Test
    fun `create nested optional type`() {
        val stringType = HIRPrimitiveType(PrimitiveKind.STRING)
        val optionalString = HIROptionalType(stringType)
        val optionalOptionalString = HIROptionalType(optionalString)
        assertTrue(optionalOptionalString.innerType is HIROptionalType)
    }

    @Test
    fun `create union type`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        val stringType = HIRPrimitiveType(PrimitiveKind.STRING)
        val unionType = HIRUnionType(listOf(intType, stringType))
        assertEquals(2, unionType.types.size)
        assertTrue(unionType.types.contains(intType))
        assertTrue(unionType.types.contains(stringType))
    }

    @Test
    fun `create function type`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        val funcType = HIRFunctionType(
            parameterTypes = listOf(intType, intType),
            returnType = intType
        )
        assertEquals(2, funcType.parameterTypes.size)
        assertEquals(intType, funcType.returnType)
    }

    @Test
    fun `create function type with no parameters`() {
        val boolType = HIRPrimitiveType(PrimitiveKind.BOOL)
        val funcType = HIRFunctionType(
            parameterTypes = emptyList(),
            returnType = boolType
        )
        assertEquals(0, funcType.parameterTypes.size)
        assertEquals(boolType, funcType.returnType)
    }

    @Test
    fun `any type singleton`() {
        val any1 = HIRAnyType
        val any2 = HIRAnyType
        assertSame(any1, any2)
    }

    @Test
    fun `type equality for primitives`() {
        val int1 = HIRPrimitiveType(PrimitiveKind.INT)
        val int2 = HIRPrimitiveType(PrimitiveKind.INT)
        assertEquals(int1, int2)
    }

    @Test
    fun `type inequality for different primitives`() {
        val intType = HIRPrimitiveType(PrimitiveKind.INT)
        val stringType = HIRPrimitiveType(PrimitiveKind.STRING)
        assertNotEquals(intType, stringType)
    }

    @Test
    fun `type equality for classes`() {
        val user1 = HIRClassType("User")
        val user2 = HIRClassType("User")
        assertEquals(user1, user2)
    }

    @Test
    fun `type inequality for different classes`() {
        val user = HIRClassType("User")
        val admin = HIRClassType("Admin")
        assertNotEquals(user, admin)
    }

    @Test
    fun `type equality for optional`() {
        val optInt1 = HIROptionalType(HIRPrimitiveType(PrimitiveKind.INT))
        val optInt2 = HIROptionalType(HIRPrimitiveType(PrimitiveKind.INT))
        assertEquals(optInt1, optInt2)
    }

    @Test
    fun `primitive kind enum completeness`() {
        val allKinds = listOf(
            PrimitiveKind.INT,
            PrimitiveKind.INT64,
            PrimitiveKind.UINT,
            PrimitiveKind.BOOL,
            PrimitiveKind.STRING,
            PrimitiveKind.VOID,
            PrimitiveKind.NULL
        )
        assertEquals(7, allKinds.size)
    }
}
