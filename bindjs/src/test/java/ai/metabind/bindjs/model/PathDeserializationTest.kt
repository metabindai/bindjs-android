package ai.metabind.bindjs.model

import ai.metabind.bindjs.GsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the wire format the JS `PathComponent` builder emits (see `script.js`) —
 * one flat `{ op, ... }` object per builder call, plus the `fill` / `stroke` props
 * that the `.fill()` / `.stroke()` shims fold into the shape rather than wrapping
 * it in a ModifiedComponent.
 */
class PathDeserializationTest {

    private fun parse(json: String): BaseComponent<*> =
        GsonProvider.get().fromJson(json, BaseComponent::class.java)

    @Test
    fun `an unstyled path parses its elements in order`() {
        val component = parse(
            """
            {"type":"Path","props":{"elements":[
              {"op":"move","x":0,"y":0},
              {"op":"line","x":100,"y":50},
              {"op":"quadCurve","x":120,"y":80,"controlX":110,"controlY":60},
              {"op":"curve","x":0,"y":80,"control1X":90,"control1Y":100,"control2X":30,"control2Y":100},
              {"op":"close"}
            ]}}
            """
        )
        assertTrue("Path must not fall through to EmptyComponent", component is PathComponent)
        val elements = (component as PathComponent).props.elements!!
        assertEquals(listOf("move", "line", "quadCurve", "curve", "close"), elements.map { it.op })

        assertEquals(100f, elements[1].x)
        assertEquals(50f, elements[1].y)
        assertEquals(110f, elements[2].controlX)
        assertEquals(30f, elements[3].control2X)
    }

    @Test
    fun `arc rect roundedRect ellipse and lines carry their own key sets`() {
        val elements = (parse(
            """
            {"type":"Path","props":{"elements":[
              {"op":"arc","centerX":50,"centerY":50,"radius":20,"startAngle":0,"endAngle":180,"clockwise":true},
              {"op":"rect","x":1,"y":2,"width":3,"height":4},
              {"op":"roundedRect","x":1,"y":2,"width":3,"height":4,"cornerWidth":5,"cornerHeight":6},
              {"op":"ellipse","x":7,"y":8,"width":9,"height":10},
              {"op":"lines","points":[[0,0],[10,10],[20,0]]}
            ]}}
            """
        ) as PathComponent).props.elements!!

        val arc = elements[0]
        assertEquals(50f, arc.centerX)
        assertEquals(20f, arc.radius)
        assertEquals(180f, arc.endAngle)
        assertEquals(true, arc.clockwise)

        assertEquals(5f, elements[2].cornerWidth)
        assertEquals(6f, elements[2].cornerHeight)
        assertEquals(9f, elements[3].width)
        assertEquals(listOf(listOf(0f, 0f), listOf(10f, 10f), listOf(20f, 0f)), elements[4].points)
    }

    @Test
    fun `fill and stroke fold into the path's own props`() {
        val props = (parse(
            """
            {"type":"Path","props":{
              "elements":[{"op":"move","x":0,"y":0}],
              "fill":{"style":{"type":"Color","props":{"rawValue":"red"}}},
              "stroke":{"style":{"type":"Color","props":{"rawValue":"blue"}},"lineWidth":4}
            }}
            """
        ) as PathComponent).props

        // The runtime spells this `lineWidth`; binding it to the bare `width` the
        // field is named after left every stroke at the 1dp fallback.
        assertEquals(4f, props.stroke?.width)
        assertEquals("red", (props.fill?.style as ColorComponent).props.rawValue)
        assertEquals("blue", (props.stroke?.style as ColorComponent).props.rawValue)
    }

    @Test
    fun `a bare stroke width arrives without a style`() {
        // `.stroke(2)` — the Stroke shim emits only a lineWidth in that case.
        val stroke = (parse(
            """{"type":"Path","props":{"elements":[],"stroke":{"lineWidth":2}}}"""
        ) as PathComponent).props.stroke
        assertEquals(2f, stroke?.width)
        assertNull(stroke?.style)
    }
}
