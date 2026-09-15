package ai.metabind.bindjs.composables.ext

import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FontModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `.font(CustomFont({ family, size, url }))`. The runtime hands the directive over twice
 * — once typed as `custom`, once verbatim as `rawValue` (see `FontModifier` in
 * script.js) — so these cover both the wire format and the size lookups that read it.
 */
class CustomFontTest {

    private fun modifier(json: String): FontModifier =
        GsonProvider.get().fromJson(json, ComponentModifier::class.java) as FontModifier

    private val customFont = modifier(
        """
        {"type":"font","props":{
          "custom":{"family":"Inter","size":24,"url":"https://example.com/Inter.ttf"},
          "rawValue":{"type":"CustomFont","props":{"family":"Inter","size":24}}
        }}
        """
    )

    @Test
    fun `a custom font's own props deserialize`() {
        val custom = customFont.props.custom
        assertEquals("Inter", custom?.family)
        assertEquals(24f, custom?.size)
        assertEquals("https://example.com/Inter.ttf", custom?.url)
    }

    @Test
    fun `getFontSize reads the size a custom font carries`() {
        // `rawValue` is the whole CustomFont directive rather than a number, so before
        // the typed props were bound this returned null and a verbatim Text rendered at
        // the inherited size — and stopped tracking the system font scale with it.
        assertEquals(24f, listOf<ComponentModifier<*>>(customFont).getFontSize())
    }

    @Test
    fun `getFontSize still reads a plain numeric font`() {
        val numeric = modifier("""{"type":"font","props":{"rawValue":20}}""")
        assertEquals(20.0, listOf<ComponentModifier<*>>(numeric).getFontSize()?.toDouble())
    }

    @Test
    fun `an inherited font does not shadow a custom font's size`() {
        // A container's `.font(...)` is shared with its children, so a child carrying
        // its own `.font(CustomFont(...))` sees both — outermost first. Reading only
        // the outermost modifier returned null here (a named style is not a number,
        // and that modifier has no custom props), and the child rendered at the
        // inherited size instead of its own.
        val inherited = modifier("""{"type":"font","props":{"rawValue":"caption"}}""")
        assertEquals(24f, listOf<ComponentModifier<*>>(inherited, customFont).getFontSize())
    }

    @Test
    fun `the innermost font wins for size`() {
        val outerNumeric = modifier("""{"type":"font","props":{"rawValue":40}}""")
        assertEquals(24f, listOf<ComponentModifier<*>>(outerNumeric, customFont).getFontSize())
    }

    @Test
    fun `getNearestFontPointSize prefers the innermost font`() {
        // Modifiers accumulate outermost-first, so the innermost `.font(...)` is last
        // and wins, as it does in SwiftUI.
        val outer = modifier("""{"type":"font","props":{"rawValue":"caption"}}""")
        assertEquals(24f, listOf<ComponentModifier<*>>(outer, customFont).getNearestFontPointSize())
        assertEquals(12f, listOf<ComponentModifier<*>>(customFont, outer).getNearestFontPointSize())
    }

    @Test
    fun `a font with no custom props is untouched`() {
        val named = modifier("""{"type":"font","props":{"rawValue":"title2"}}""")
        assertNull(named.props.custom)
        assertNull(listOf<ComponentModifier<*>>(named).getFontSize())
        assertEquals(22f, listOf<ComponentModifier<*>>(named).getNearestFontPointSize())
    }

    @Test
    fun `customFontProps finds the nearest custom font`() {
        val named = modifier("""{"type":"font","props":{"rawValue":"body"}}""")
        assertEquals("Inter", listOf<ComponentModifier<*>>(named, customFont).customFontProps()?.family)
        assertTrue(listOf<ComponentModifier<*>>(named).customFontProps() == null)
    }
}
