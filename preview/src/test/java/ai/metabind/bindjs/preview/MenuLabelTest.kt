package ai.metabind.bindjs.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * `Menu('Actions', [...])` sends its title as `rawValue`, not as a `label` component, and
 * the menu used to draw nothing at all: no title and, with nothing to tap, no way to open
 * it. A submenu written the same way lost its title inside the open menu too.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class MenuLabelTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "Menu" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)

        compose.setContent {
            MaterialTheme {
                BindJSView(
                    jsRuntime = FakeJsRuntime(),
                    component = tree,
                    version = 1,
                    onUiEvent = {},
                )
            }
        }
    }

    @Test
    fun `a string title is the menu's label and opens it`() {
        compose.onNodeWithText("Actions").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Copy").assertIsDisplayed()
    }

    @Test
    fun `a string-titled submenu shows its title in the open menu`() {
        compose.onNodeWithText("Actions").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun `a label component is still used as given`() {
        compose.onNodeWithText("Styled label").assertIsDisplayed()
    }

    @Test
    fun `a menu with no label reads Menu, as on iOS`() {
        compose.onNodeWithText("Menu").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Untitled item").assertIsDisplayed()
    }
}
