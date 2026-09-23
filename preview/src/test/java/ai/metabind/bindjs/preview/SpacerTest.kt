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
 * The `Spacer` fixture, measured: where each Spacer leaves its neighbours. A screenshot
 * shows this too, but a gap that is a few dp off would pass the screenshot threshold.
 *
 * Stacks here use the default spacing of 8dp on each side of the Spacer, so a Spacer
 * `n` wide leaves `n + 16` between its neighbours.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class SpacerTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "Spacer" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)
        compose.setContent {
            MaterialTheme {
                BindJSView(jsRuntime = FakeJsRuntime(), component = tree, version = 1, onUiEvent = {})
            }
        }
    }

    private fun bounds(text: String): DpRect = compose.onNodeWithText(text).getBoundsInRoot()

    /** The distance a 300dp-wide bar spans from its first text to its last. */
    private fun span(left: String, right: String) = (bounds(right).right - bounds(left).left).value

    private fun gap(before: String, after: String, vertical: Boolean = false): Float =
        if (vertical) (bounds(after).top - bounds(before).bottom).value
        else (bounds(after).left - bounds(before).right).value

    @Test
    fun `a bare Spacer pushes its neighbours to the edges`() =
        assertEquals(300f, span("A1", "B1"), 1f)

    @Test
    fun `minLength is a floor, not a size`() =
        assertEquals(300f, span("A2", "B2"), 1f)

    @Test
    fun `a padded Spacer still takes the slack`() =
        assertEquals(300f, span("A3", "B3"), 1f)

    @Test
    fun `a Spacer under a flexible frame still takes the slack`() =
        assertEquals(300f, span("A4", "B4"), 1f)

    @Test
    fun `a Spacer in a Group still takes the slack`() =
        assertEquals(300f, span("A5", "B5"), 1f)

    @Test
    fun `a fixed-width frame keeps a Spacer its size`() =
        assertEquals(24f + 16f, gap("A6", "B6"), 1f)

    @Test
    fun `a Spacer stretches a stack in a fixed-size card`() =
        assertEquals(90f, (bounds("Bottom C1").bottom - bounds("Top C1").top).value, 1f)

    @Test
    fun `a Spacer stretches a stack nested in a fixed-size card`() =
        assertEquals(90f, (bounds("Bottom C2").bottom - bounds("Top C2").top).value, 1f)

    @Test
    fun `in a vertical ScrollView a Spacer is its minimum`() =
        assertEquals(24f + 16f, gap("Top S", "Bottom S", vertical = true), 1f)

    @Test
    fun `in a horizontal ScrollView a Spacer is its minimum`() =
        assertEquals(30f + 16f, gap("Left H", "Right H"), 1f)
}
