package ai.metabind.bindjs.model

import ai.metabind.bindjs.GsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the wire format the runtime emits for `Slider({...})` — a generic built-in, so
 * `setValue` has been rewritten to `setValueId` by `processProps` and the two value
 * labels arrive as nested directives rather than strings.
 */
class SliderDeserializationTest {

    private fun parse(json: String): BaseComponent<*> =
        GsonProvider.get().fromJson(json, BaseComponent::class.java)

    @Test
    fun `a fully specified slider parses every prop`() {
        val component = parse(
            """
            {"type":"Slider","props":{
              "value":25,
              "range":[0,100],
              "step":5,
              "label":"Volume",
              "minimumValueLabel":{"type":"Text","props":{"rawValue":"0","children":[]}},
              "maximumValueLabel":{"type":"Text","props":{"rawValue":"100","children":[]}},
              "setValueId":"0,Probe_0,VStack_0,Slider_0,setValue,0",
              "children":[]
            }}
            """
        )
        assertTrue("Slider must not fall through to EmptyComponent", component is SliderComponent)
        val props = (component as SliderComponent).props

        assertEquals(25.0, props.value!!, 0.0)
        assertEquals(listOf(0.0, 100.0), props.range)
        assertEquals(5.0, props.step!!, 0.0)
        assertEquals("Volume", props.label)
        assertEquals("0,Probe_0,VStack_0,Slider_0,setValue,0", props.setValueId)
        assertEquals("0", (props.minimumValueLabel as TextComponent).props.rawValue)
        assertEquals("100", (props.maximumValueLabel as TextComponent).props.rawValue)
    }

    @Test
    fun `a bare slider leaves the optional props null`() {
        val props = (parse(
            """{"type":"Slider","props":{"value":0.5,"setValueId":"x","children":[]}}"""
        ) as SliderComponent).props

        assertEquals(0.5, props.value!!, 0.0)
        assertNull(props.range)
        assertNull(props.step)
        assertNull(props.label)
        assertNull(props.minimumValueLabel)
        assertNull(props.maximumValueLabel)
    }
}
