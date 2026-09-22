package ai.metabind.bindjs.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.composables.UiEvent
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
 * `.disabled(true)` behaves as SwiftUI's `isEnabled` environment value: it disables every
 * control below it, and nothing further in can re-enable it.
 *
 * Before, it only disabled the control it was written on, and only a Button, TextField,
 * Slider, Picker or Video; a Toggle, NavigationLink or Menu ignored it entirely. Every
 * control in the `Disabled` fixture except the last is disabled, so a tap on any of them
 * must reach nothing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class DisabledTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<UiEvent>()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "Disabled" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)

        compose.setContent {
            MaterialTheme {
                BindJSView(
                    jsRuntime = FakeJsRuntime(),
                    component = tree,
                    version = 1,
                    onUiEvent = { if (it !is UiEvent.OnAppear && it !is UiEvent.OnDisappear) events += it },
                )
            }
        }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun assertNothingReached(vararg labels: String) {
        labels.forEach { tap(it) }
        assertEquals("events from ${labels.toList()}", emptyList<UiEvent>(), events)
    }

    @Test
    fun `a disabled stack disables the buttons inside it`() =
        assertNothingReached("Stack button")

    @Test
    fun `a disabled stack disables a toggle inside it`() {
        // The label is plain text beside the switch; only the switch takes the tap.
        compose.onNode(isToggleable()).performClick()
        compose.waitForIdle()
        assertEquals(emptyList<UiEvent>(), events)
    }

    @Test
    fun `a disabled stack disables a segmented picker inside it`() =
        assertNothingReached("Seg B")

    @Test
    fun `a disabled stack disables a navigation link inside it`() =
        assertNothingReached("Stack link")

    @Test
    fun `a disabled stack disables a text field inside it`() {
        compose.onNodeWithText("Stack field").assertIsNotEnabled()
    }

    @Test
    fun `a disabled stack keeps a menu inside it closed`() {
        tap("Stack menu")
        assertEquals(0, compose.onAllNodesWithText("Menu item").fetchSemanticsNodes().size)
    }

    @Test
    fun `a disabled false inside a disabled stack does not re-enable`() =
        assertNothingReached("Nested enable")

    @Test
    fun `a disabled ScrollView and List disable their rows`() =
        assertNothingReached("Scroll button", "List button")

    @Test
    fun `a disabled modifier behind a frame still applies`() =
        assertNothingReached("Framed button")

    @Test
    fun `a disabled false further out does not re-enable`() =
        assertNothingReached("Inner disabled")

    @Test
    fun `an enabled button still taps`() {
        tap("Live button")
        assertEquals(1, events.count { it is UiEvent.OnTap })
    }
}
