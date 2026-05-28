package ai.metabind.bindjs.model.modifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `foregroundStyle`'s `rawValue` is typed `Any?`, so Gson deserializes nested directives
 * as plain Maps rather than polymorphic Components. These cover the shapes that reach
 * [asColorComponent] at runtime.
 */
class ForegroundStyleColorResolutionTest {

    @Test
    fun `resolves a direct Color map`() {
        val rawValue = mapOf(
            "type" to "Color",
            "props" to mapOf("rawValue" to "white"),
        )
        assertEquals("white", rawValue.asColorComponent()?.props?.rawValue)
    }

    @Test
    fun `resolves a wrapper ComponentCall map that holds a Color child`() {
        // Mirrors the AST produced by `.foregroundStyle(CardTextColor(Color('white')))`.
        val rawValue = mapOf(
            "type" to "ComponentCall",
            "props" to mapOf(
                "name" to "CardTextColor",
                "props" to emptyMap<String, Any>(),
                "children" to listOf(
                    mapOf("type" to "Color", "props" to mapOf("rawValue" to "white")),
                ),
            ),
        )
        val resolved = rawValue.asColorComponent()
        assertNotNull("ComponentCall wrapping a Color should resolve to that color", resolved)
        assertEquals("white", resolved?.props?.rawValue)
    }

    @Test
    fun `resolves a nested rgb Color child`() {
        val rawValue = mapOf(
            "type" to "ComponentCall",
            "props" to mapOf(
                "children" to listOf(
                    mapOf(
                        "type" to "Color",
                        "props" to mapOf("r" to 34, "g" to 199, "b" to 25, "a" to 1),
                    ),
                ),
            ),
        )
        val resolved = rawValue.asColorComponent()
        assertNotNull(resolved)
        assertEquals(34f, resolved?.props?.r)
        assertEquals(199f, resolved?.props?.g)
    }

    @Test
    fun `returns null for a wrapper without any color`() {
        val rawValue = mapOf(
            "type" to "ComponentCall",
            "props" to mapOf(
                "children" to listOf(
                    mapOf("type" to "Text", "props" to mapOf("rawValue" to "hi")),
                ),
            ),
        )
        assertNull(rawValue.asColorComponent())
    }
}
