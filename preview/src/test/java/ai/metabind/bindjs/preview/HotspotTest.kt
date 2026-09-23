package ai.metabind.bindjs.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A Text's view-wide effects apply once. TextView builds the modifier chain for both its
 * wrapping Box and the text node, so an offset moved a label twice as far as asked — the
 * Explore card's hotspot labels hung 64pt below their dots instead of 32 — and a scale
 * compounded. The `Hotspot` fixture's screenshot shows the dots, the labels and the
 * faded and scaled Text (doubled, the scaled one clips and the faded one comes out
 * lighter); this measures the offset, which a screenshot threshold could let slip.
 * Semantics bounds do not see the inner layer's scale, so scale has only the screenshot.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class HotspotTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "Hotspot" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)
        compose.setContent {
            MaterialTheme {
                BindJSView(jsRuntime = FakeJsRuntime(), component = tree, version = 1, onUiEvent = {})
            }
        }
    }

    private fun bounds(text: String): DpRect = compose.onNodeWithText(text).getBoundsInRoot()

    private fun DpRect.centerY() = ((top + bottom) / 2).value

    @Test
    fun `an offset moves a Text once`() =
        // `Text('Offset').offset({ y: 12 })` beside an un-offset `Text('Faded')`.
        assertEquals(12f, bounds("Offset").centerY() - bounds("Faded").centerY(), 1f)
}
