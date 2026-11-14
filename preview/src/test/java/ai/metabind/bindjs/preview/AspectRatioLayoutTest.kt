package ai.metabind.bindjs.preview

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
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

    /**
     * The size [box] takes in a parent [width] wide and [height] tall, or unbounded in
     * height, around content that fills what it is offered, as a color does.
     */
    private fun sizeIn(width: Int, height: Int?, box: Modifier): IntSize {
        compose.setContent {
            val parent = if (height != null) Modifier.size(width.dp, height.dp)
            else Modifier.width(width.dp).verticalScroll(rememberScrollState())
            Box(parent) {
                Box(box.testTag("box").fillMaxSize())
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
    fun `a parent sizing by intrinsics gets the box`() {
        // A card sized to its content's intrinsic height (IntrinsicSize, as a grid row or
        // an equal-height stack measures) counts the box, not the color inside it.
        compose.setContent {
            Box(Modifier.width(360.dp).height(IntrinsicSize.Max).testTag("box")) {
                Box(Modifier.aspectRatioBox(2f, fill = false).fillMaxSize())
            }
        }
        assertEquals(180, compose.onNodeWithTag("box").fetchSemanticsNode().size.height)
    }

    @Test
    fun `a resizable image keeps the box under a parent sizing by intrinsics`() {
        // A resizable image takes any size, so the square box around this 2:1 image stays
        // square when a parent asks for its intrinsic height, before and after it loads.
        val box = GsonProvider.get().fromJson(
            modified("aspectRatio", "\"aspectRatio\":1,\"contentMode\":\"fit\"", WIDE_IMAGE),
            BaseComponent::class.java,
        )
        compose.setContent {
            Box(Modifier.width(360.dp).height(IntrinsicSize.Max).testTag("root")) {
                BindJSView(jsRuntime = FakeJsRuntime(), component = box, version = 1, onUiEvent = {})
            }
        }
        settle(Duration.ofSeconds(3))
        assertEquals(360f, rootHeight(), 0f)
    }

    @Test
    fun `a view with a size of its own keeps it`() {
        // SwiftUI proposes the fitted box, 360 x 180, and the 40 x 20 frame keeps its size.
        render(modified("scaledToFit", "", modified("frame", "\"width\":40,\"height\":20", RED)))
        assertEquals(20f, rootHeight(), 0f)
    }

    @Test
    fun `a GeometryReader takes the box`() {
        // A GeometryReader is flexible in SwiftUI; Compose's wraps its content, which it
        // only has once it knows its size, so a box that let it choose drew nothing.
        val reader = """{"type":"GeometryReader","props":{"handlerId":"h","environmentId":"e"}}"""
        render(modified("aspectRatio", "\"aspectRatio\":1.25,\"contentMode\":\"fit\"", reader))
        assertEquals(288f, rootHeight(), 0f)
    }

    @Test
    fun `a lazy list keeps its own ratio without intrinsic measurements`() {
        // A LazyColumn answers no intrinsic measurements; asking threw.
        val list = """{"type":"List","props":{"children":[{"type":"Text","props":{"rawValue":"row"}}]}}"""
        render(modified("scaledToFit", "", list), height = 400)
        assertTrue(rootHeight() > 0f)
    }

    @Test
    fun `a filled box of an extreme ratio is cut to what Compose can measure`() {
        // SwiftUI's box is 280,000 x 400, longer than Compose can represent; asking for it threw.
        assertEquals(400, sizeIn(360, 400, Modifier.aspectRatioBox(700f, fill = true)).height)
    }

    private fun render(tree: String, density: Density? = null, height: Int? = null) {
        val component = GsonProvider.get().fromJson(tree, BaseComponent::class.java)
        compose.setContent {
            val content = @Composable {
                MaterialTheme {
                    val root = if (height != null) Modifier.size(360.dp, height.dp) else Modifier.width(360.dp).verticalScroll(rememberScrollState())
                    Box(root.testTag("root")) {
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
        val image = WIDE_IMAGE
        render(modified("frame", """"width":360,"alignment":"topLeading"""", modified("scaledToFit", "", image)))
        assertEquals(180f, settledHeight(180f), 0f)
    }

    @Test
    fun `an image's ratio includes the padding around it`() {
        // The padded image's ideal size is 60 x 40, so a 360-wide box is 240 tall.
        render(modified("scaledToFit", "", modified("padding", "\"rawValue\":10", WIDE_IMAGE)))
        assertEquals(240f, settledHeight(240f), 0f)
    }

    @Test
    fun `an image's ratio reaches a box outside its frame`() {
        // The aspect box sits on the frame's layout, not the image's; it still remeasures
        // when the image loads.
        render(modified("scaledToFit", "", modified("frame", "\"maxWidth\":\"Infinity\"", WIDE_IMAGE)))
        assertEquals(180f, settledHeight(180f), 0f)
    }

    /** Lets Coil load off the main thread for [time]. */
    private fun settle(time: Duration) {
        val deadline = System.currentTimeMillis() + time.toMillis()
        while (System.currentTimeMillis() < deadline) {
            compose.waitForIdle()
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }
    }

    /** The root's height once it reaches [expected] or 10 seconds pass: Coil loads off the main thread. */
    private fun settledHeight(expected: Float): Float {
        val deadline = System.currentTimeMillis() + 10_000
        while (rootHeight() != expected && System.currentTimeMillis() < deadline) {
            compose.waitForIdle()
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }
        return rootHeight()
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

    @Test
    fun `tracking written outside a frame sets the text inside`() {
        // Ten letters with 5pt tracking added outside a frame: about 45 points wider.
        val plain = """{"type":"Text","props":{"rawValue":"$LETTERS"}}"""
        val framed = modified("tracking", """"rawValue":5""", modified("frame", """"width":300""", plain))
        render("""{"type":"VStack","props":{"children":[$plain,$framed]}}""")
        val texts = compose.onAllNodesWithText(LETTERS)
        val added = width(texts[1].getBoundsInRoot()) - width(texts[0].getBoundsInRoot())
        assertTrue("tracking added $added", added in 44f..51f)
    }

    @Test
    fun `the innermost tracking wins`() {
        // 5pt on the text, 20pt outside its frame: the text's own 5pt applies, as in SwiftUI.
        val plain = """{"type":"Text","props":{"rawValue":"$LETTERS"}}"""
        val inner = modified("tracking", """"rawValue":5""", plain)
        val both = modified("tracking", """"rawValue":20""", modified("frame", """"width":360""", inner))
        render("""{"type":"VStack","props":{"children":[$plain,$both]}}""")
        val texts = compose.onAllNodesWithText(LETTERS)
        val added = width(texts[1].getBoundsInRoot()) - width(texts[0].getBoundsInRoot())
        assertTrue("tracking added $added", added in 44f..51f)
    }

    companion object {
        const val LETTERS = "MMMMMMMMMM"

        const val RED = """{"type":"Color","props":{"rawValue":"red"}}"""

        /** A 40 x 20 magenta PNG. */
        const val WIDE_PNG =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAJElEQVR4nGP4z/B/QBDDqMWjFo9aPGrxqMWjFo9aPGrxyLEYALhmOhvE9gcGAAAAAElFTkSuQmCC"

        const val WIDE_IMAGE = """{"type":"Image","props":{"url":"$WIDE_PNG","resizable":true}}"""
    }
}
