package ai.metabind.bindjs.model

import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.BorderModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.asColorComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the wire format the runtime emits for `Material(...)` in each slot a style can
 * go, and the single-style `.border(style)` form that used to fail the whole tree parse.
 */
class MaterialDecodingTest {

    private fun parse(json: String): BaseComponent<*> =
        GsonProvider.get().fromJson(json, BaseComponent::class.java)

    private fun material(name: String) =
        """{"type":"Material","props":{"rawValue":"$name","children":[]}}"""

    private fun modified(modifier: String) =
        """{"type":"ModifiedComponent","props":{"modifier":$modifier,
            "content":[{"type":"Text","props":{"rawValue":"x","children":[]}}]}}"""

    @Test
    fun `a standalone material is a colour`() {
        val component = parse(material("thin"))
        assertTrue(component is MaterialComponent)
        assertTrue("every ColorComponent branch must take it", component is ColorComponent)
        assertEquals("thin", (component as MaterialComponent).props.rawValue)
    }

    @Test
    fun `a material background decodes to a material`() {
        val component = parse(
            modified("""{"type":"background","props":{"content":${material("ultraThin")},"children":[]}}""")
        ) as ModifiedComponent
        val modifier = component.props.modifier as BackgroundModifier
        assertTrue(modifier.props.content is MaterialComponent)
    }

    @Test
    fun `a material foreground style resolves through the untyped map`() {
        val component = parse(
            modified("""{"type":"foregroundStyle","props":{"rawValue":${material("thick")},"children":[]}}""")
        ) as ModifiedComponent
        val modifier = component.props.modifier as ForegroundStyleModifier
        val resolved = modifier.props.rawValue.asColorComponent()
        assertTrue(resolved is MaterialComponent)
        assertEquals("thick", resolved!!.props.rawValue)
    }

    @Test
    fun `a material shape fill decodes to a material`() {
        val component = parse(
            """{"type":"Capsule","props":{"children":[],"fill":{"style":${material("regular")}}}}"""
        ) as CapsuleComponent
        assertTrue(component.props.fill?.style is MaterialComponent)
    }

    @Test
    fun `a single-style border parses and keeps its style`() {
        val component = parse(
            modified("""{"type":"border","props":{"rawValue":{"type":"Color","props":{"rawValue":"red","children":[]}},"children":[]}}""")
        ) as ModifiedComponent
        val modifier = component.props.modifier as BorderModifier
        assertTrue(modifier.props.rawValue!!.isJsonObject)
        assertNull(modifier.props.style)
    }

    @Test
    fun `an object border keeps style and width`() {
        val component = parse(
            modified("""{"type":"border","props":{"style":${material("thin")},"width":2,"children":[]}}""")
        ) as ModifiedComponent
        val modifier = component.props.modifier as BorderModifier
        assertTrue(modifier.props.style is MaterialComponent)
        assertEquals(2f, modifier.props.width!!, 0f)
    }

    @Test
    fun `a numeric border keeps its width`() {
        val component = parse(
            modified("""{"type":"border","props":{"rawValue":3,"children":[]}}""")
        ) as ModifiedComponent
        val modifier = component.props.modifier as BorderModifier
        assertEquals(3f, modifier.props.rawValue!!.asFloat, 0f)
    }

    @Test
    fun `materials thicken in order and unknown names draw nothing`() {
        val alphas = listOf("ultraThin", "thin", "regular", "thick", "ultraThick")
            .map { MaterialComponent.tintFor(it) ushr 24 }
        assertEquals(alphas.sorted(), alphas)
        assertEquals(alphas.size, alphas.toSet().size)
        // bindjs-apple parses only those five; `bar`, `chrome` and a missing name are nil there.
        listOf("bar", "chrome", null).forEach { assertEquals(0, MaterialComponent.tintFor(it)) }
    }
}
