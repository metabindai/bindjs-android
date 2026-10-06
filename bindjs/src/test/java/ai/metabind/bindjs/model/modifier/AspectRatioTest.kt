package ai.metabind.bindjs.model.modifier

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.ext.getContentScale
import ai.metabind.bindjs.composables.ext.getTracking
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ModifiedComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The aspect modifiers' wire format and box arithmetic, and how an image draws into the
 * box. The layout itself is measured in the preview module's `AspectRatioLayoutTest`.
 */
class AspectRatioTest {

    private fun modifier(type: String, props: String = ""): ComponentModifier<*> {
        val json = """{"type":"ModifiedComponent","props":{"modifier":{"type":"$type","props":{$props${if (props.isEmpty()) "" else ","}"children":[]}},"content":[{"type":"Color","props":{"rawValue":"red","children":[]}}]}}"""
        val component = GsonProvider.get().fromJson(json, BaseComponent::class.java) as ModifiedComponent
        return component.props.modifier!!
    }

    private fun aspect(props: String) = (modifier("aspectRatio", props) as AspectRatioModifier).props

    @Test
    fun `reads the ratio and content mode the runtime sends`() {
        val props = aspect(""""aspectRatio":1.5,"contentMode":"fill"""")
        assertEquals(1.5f, props.ratio!!, 0f)
        assertEquals("fill", props.contentMode)
    }

    @Test
    fun `reads the ratio an older runtime sends as rawValue`() {
        assertEquals(2f, aspect(""""rawValue":2""").ratio!!, 0f)
    }

    @Test
    fun `a missing or null ratio keeps the content's own`() {
        assertNull(aspect(""""contentMode":"fit"""").ratio)
        assertNull(aspect(""""aspectRatio":null,"contentMode":"fit"""").ratio)
    }

    @Test
    fun `a ratio that cannot size a box keeps the content's own`() {
        for (bad in listOf("0", "-1", "\"Infinity\"", "\"NaN\"")) {
            assertNull(bad, aspect(""""aspectRatio":$bad""").ratio)
        }
    }

    @Test
    fun `a null length anywhere in a tree parses`() {
        // A null used to throw from the Float adapter and fail the whole tree.
        val json = """{"type":"ModifiedComponent","props":{"modifier":{"type":"frame","props":{"width":null,"height":40,"children":[]}},"content":[{"type":"Color","props":{"rawValue":"red","children":[]}}]}}"""
        val frame = (GsonProvider.get().fromJson(json, BaseComponent::class.java) as ModifiedComponent).props.modifier as FrameModifier
        assertNull(frame.props.width)
        assertEquals(40f, frame.props.height!!, 0f)
    }

    @Test
    fun `fit is the largest box inside the offer`() {
        assertEquals(IntSize(200, 100), aspectBox(360, 100, 2f, fill = false))
        assertEquals(IntSize(360, 180), aspectBox(360, 400, 2f, fill = false))
    }

    @Test
    fun `fill is the smallest box covering the offer`() {
        assertEquals(IntSize(360, 180), aspectBox(360, 100, 2f, fill = true))
        assertEquals(IntSize(800, 400), aspectBox(360, 400, 2f, fill = true))
    }

    @Test
    fun `one offered length sizes the box whatever the mode`() {
        for (fill in listOf(false, true)) {
            assertEquals(IntSize(360, 180), aspectBox(360, null, 2f, fill))
            assertEquals(IntSize(200, 100), aspectBox(null, 100, 2f, fill))
        }
    }

    @Test
    fun `nothing offered leaves the box to the content`() {
        assertNull(aspectBox(null, null, 2f, fill = false))
    }

    @Test
    fun `an image draws into its box by the innermost aspect modifier`() {
        assertEquals(ContentScale.Fit, listOf(modifier("scaledToFit")).getContentScale())
        assertEquals(ContentScale.Crop, listOf(modifier("scaledToFill")).getContentScale())
        assertEquals(ContentScale.Fit, listOf(modifier("aspectRatio", """"contentMode":"fit"""")).getContentScale())
        assertEquals(ContentScale.Crop, listOf(modifier("aspectRatio", """"contentMode":"fill"""")).getContentScale())
        // An explicit ratio stretches the image into a box of that ratio, as in SwiftUI.
        assertEquals(ContentScale.FillBounds, listOf(modifier("aspectRatio", """"aspectRatio":2,"contentMode":"fit"""")).getContentScale())
        // The list runs outermost first; the modifier nearest the image decides.
        assertEquals(ContentScale.Crop, listOf(modifier("scaledToFit"), modifier("scaledToFill")).getContentScale())
        assertEquals(ContentScale.Fit, emptyList<ComponentModifier<*>>().getContentScale())
    }

    @Test
    fun `the innermost tracking wins`() {
        val outer = modifier("tracking", """"rawValue":1""")
        val inner = modifier("tracking", """"rawValue":3""")
        assertEquals(3f, listOf(outer, inner).getTracking(), 0f)
        assertEquals(0f, emptyList<ComponentModifier<*>>().getTracking(), 0f)
    }
}
