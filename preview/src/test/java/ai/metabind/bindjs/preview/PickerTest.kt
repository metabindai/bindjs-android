package ai.metabind.bindjs.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.routeUiEvent
import ai.metabind.bindjs.model.BaseComponent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The `Picker` fixture, driven: what a screenshot at rest cannot show is which taps
 * reach the setter. Each case here was wrong before — a segmented style written on the
 * stack or behind a frame fell back to the menu, an untagged option vanished, a
 * disabled picker still changed the selection.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class PickerTest {

    @get:Rule
    val compose = createComposeRule()

    private val taps = mutableListOf<UiEvent.OnPickerTap>()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "Picker" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)

        compose.setContent {
            MaterialTheme {
                BindJSView(
                    jsRuntime = FakeJsRuntime(),
                    component = tree,
                    version = 1,
                    onUiEvent = { if (it is UiEvent.OnPickerTap) taps += it },
                )
            }
        }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun `a segmented style set on the stack reaches the picker`() {
        listOf("Small", "Medium", "Large").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        tap("Large")
        assertEquals(listOf("l"), taps.map { it.tag })
    }

    @Test
    fun `a segmented style behind a frame still applies`() {
        compose.onNodeWithText("Slow").assertIsDisplayed()
        compose.onNodeWithText("Fast").assertIsDisplayed()
    }

    @Test
    fun `a picker's own style beats the one it inherits`() {
        // A menu shows only its selection until it is opened.
        compose.onNodeWithText("Men's").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Women's").fetchSemanticsNodes().size)
    }

    @Test
    fun `an untagged option is shown but cannot be chosen`() {
        compose.onNodeWithText("Untagged").assertIsDisplayed()
        tap("Untagged")
        assertEquals(emptyList<String>(), taps.map { it.tag })
        tap("Tagged")
        assertEquals(listOf("tagged"), taps.map { it.tag })
    }

    @Test
    fun `a disabled picker does not change the selection`() {
        compose.onNodeWithText("Off").assertIsDisplayed()
        tap("Off")
        assertEquals(emptyList<String>(), taps.map { it.tag })
    }

    @Test
    fun `choosing from the menu sends the picked tag`() {
        tap("Men's")
        tap("Women's")
        assertEquals(listOf("Women's"), taps.map { it.tag })
    }

    @Test
    fun `a pick restores the picker's environment before calling its setter`() {
        val runtime = FakeJsRuntime()
        runBlocking {
            runtime.routeUiEvent(UiEvent.OnPickerTap(environmentId = "env", setterId = "set", tag = "Men's"))
        }
        assertEquals(listOf("restoreEnvironment(env)", "callPickerSetter(set, Men's)"), runtime.calls)
    }
}
