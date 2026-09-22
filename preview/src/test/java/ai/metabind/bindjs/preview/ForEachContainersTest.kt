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
 * A `ForEach` written inside a container that reads its children — a picker's options,
 * a menu's items, a toolbar's items — contributes its rows, as it does in SwiftUI.
 *
 * With `expandForEach: true` the rows arrive as one `ForEach` child, and each of these
 * containers used to skip it: the pickers had no options, the menu opened empty and the
 * toolbar lost its buttons. The screenshot of the `ForEach` fixture shows the toolbar and
 * the segments; the menus only show their contents once opened, which is done here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class ForEachContainersTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun render() {
        val fixture = componentFixtures.first { it.name == "ForEach" }
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
    fun `toolbar items from a ForEach are in the bar`() {
        compose.onNodeWithText("Share").assertIsDisplayed()
        compose.onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun `segmented picker options from a ForEach are segments`() {
        listOf("S", "M", "L", "XL").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun `menu picker shows its selection and lists the ForEach options`() {
        compose.onNodeWithText("Cherry").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Apple").assertIsDisplayed()
        compose.onNodeWithText("Banana").assertIsDisplayed()
    }

    @Test
    fun `menu lists the ForEach items`() {
        compose.onNodeWithText("Actions").performClick()
        compose.waitForIdle()
        listOf("Copy", "Rename", "Delete").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }
}
