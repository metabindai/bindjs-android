package ai.metabind.bindjs.preview

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.modifier.LocalContentRatio
import ai.metabind.bindjs.model.modifier.aspectRatioBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * `aspectRatio`, `scaledToFit` and `scaledToFill` measured against SwiftUI's sizes, and
 * tracking measured under a large font scale. At mdpi a dp is a pixel, so sizes compare
 * exactly.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-mdpi")
class AspectRatioLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    /** The size [box] takes in a parent [width] wide and [height] tall, or unbounded in height. */
    private fun sizeIn(width: Int, height: Int?, box: Modifier): IntSize {
        compose.setContent {
            val parent = if (height != null) Modifier.size(width.dp, height.dp)
            else Modifier.width(width.dp).verticalScroll(rememberScrollState())
            Box(parent) {
                Box(box.testTag("box"))
            }
        }
        return compose.onNodeWithTag("box").fetchSemanticsNode().size
    }

    @Test
    fun `fit takes the largest box of the ratio inside the offer`() =
        assertEquals(IntSize(200, 100), sizeIn(360, 100, Modifier.aspectRatioBox(2f, fill = false)))

    @Test
    fun `fill takes the smallest box covering the offer`() =
        assertEquals(IntSize(360, 180), sizeIn(360, 100, Modifier.aspectRatioBox(2f, fill = true)))

    @Test
    fun `an offered width alone derives the height`() =
        assertEquals(IntSize(360, 180), sizeIn(360, null, Modifier.aspectRatioBox(2f, fill = false)))

    @Test
    fun `without a ratio a color is square`() =
        assertEquals(IntSize(100, 100), sizeIn(300, 100, Modifier.aspectRatioBox(null, fill = false)))

    @Test
    fun `scaledToFit keeps the ratio the content provides`() {
        val scaledToFit = GsonProvider.get().fromJson(
            modified("scaledToFit", "", """{"type":"Color","props":{"rawValue":"red"}}"""),
            BaseComponent::class.java,
        ) as ModifiedComponent
        compose.setContent {
            Box(Modifier.size(360.dp, 100.dp)) {
                CompositionLocalProvider(LocalContentRatio provides 0.5f) {
                    Box(scaledToFit.props.modifier!!.buildModifier {}.testTag("box"))
                }
            }
        }
        assertEquals(IntSize(50, 100), compose.onNodeWithTag("box").fetchSemanticsNode().size)
    }

    private fun render(tree: String, density: Density? = null) {
        val component = GsonProvider.get().fromJson(tree, BaseComponent::class.java)
        compose.setContent {
            val content = @Composable {
                MaterialTheme {
                    Box(Modifier.width(360.dp).verticalScroll(rememberScrollState()).testTag("root")) {
                        BindJSView(jsRuntime = FakeJsRuntime(), component = component, version = 1, onUiEvent = {})
                    }
                }
            }
            if (density != null) CompositionLocalProvider(LocalDensity provides density, content = content) else content()
        }
    }

    private fun width(bounds: DpRect) = (bounds.right - bounds.left).value

    private fun rootHeight() = compose.onNodeWithTag("root").getBoundsInRoot().let { (it.bottom - it.top).value }

    private fun modified(type: String, props: String, content: String) =
        """{"type":"ModifiedComponent","props":{"modifier":{"type":"$type","props":{$props}},"content":[$content]}}"""

    @Test
    fun `scaledToFit sizes an image's box to the loaded image`() {
        // 40 x 20 pixels: once loaded, a 360-wide box is 180 tall, not the square it
        // starts as. Coil doesn't remeasure the layouts reading the image's intrinsic size,
        // so this stayed 360 tall before the image provided its ratio.
        val image = """{"type":"Image","props":{"url":"$WIDE_PNG","resizable":true}}"""
        render(modified("frame", """"width":360,"alignment":"topLeading"""", modified("scaledToFit", "", image)))
        val deadline = System.currentTimeMillis() + 10_000
        while (rootHeight() != 180f && System.currentTimeMillis() < deadline) {
            compose.waitForIdle()
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }
        assertEquals(180f, rootHeight(), 0f)
    }

    @Test
    fun `tracking is in points and ignores the font scale`() {
        // Ten letters, one set with 5pt tracking: about 50 points wider at a 2x font scale
        // too (Android leaves out the last letter's spacing). As sp, the spacing doubled
        // with the font scale, to about 90.
        val plain = """{"type":"Text","props":{"rawValue":"$LETTERS"}}"""
        val tracked = modified("tracking", """"rawValue":5""", plain)
        render("""{"type":"VStack","props":{"children":[$plain,$tracked]}}""", Density(1f, fontScale = 2f))
        val texts = compose.onAllNodesWithText(LETTERS)
        val added = width(texts[1].getBoundsInRoot()) - width(texts[0].getBoundsInRoot())
        assertTrue("tracking added $added", added in 44f..51f)
    }

    companion object {
        const val LETTERS = "MMMMMMMMMM"

        /** A 40 x 20 magenta PNG. */
        const val WIDE_PNG =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAJElEQVR4nGP4z/B/QBDDqMWjFo9aPGrxqMWjFo9aPGrxyLEYALhmOhvE9gcGAAAAAElFTkSuQmCC"
    }
}
