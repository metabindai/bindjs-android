package ai.metabind.bindjs.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A vertical ScrollView or a List inside a `.frame(width:height:)` renders.
 *
 * Such a frame measures its content with unbounded height so that tall content can
 * overflow the slot, and a vertically scrolling list measured that way throws —
 * "Vertically scrollable component was measured with an infinity maximum height". Any
 * `ScrollView(...).frame({ width, height })` took the whole screen down with it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class FramedScrollViewTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(json: String) {
        val tree = GsonProvider.get().fromJson(json, BaseComponent::class.java)
        compose.setContent {
            MaterialTheme {
                BindJSView(jsRuntime = FakeJsRuntime(), component = tree, version = 1, onUiEvent = {})
            }
        }
    }

    /** `ScrollView([Text('Scrolled row')]).frame({ width: 200, height: 90 })` */
    @Test
    fun `a ScrollView in a width and height frame renders`() {
        render(
            """{"type":"ModifiedComponent","props":{"modifier":{"type":"frame","props":{"width":200,"height":90,"children":[]}},"content":[{"type":"ScrollView","props":{"children":[{"type":"Text","props":{"rawValue":"Scrolled row","children":[]}}]}}]}}"""
        )
        compose.onNodeWithText("Scrolled row").assertIsDisplayed()
    }

    /** `List([Text('Listed row')]).frame({ width: 200, height: 90 })` */
    @Test
    fun `a List in a width and height frame renders`() {
        render(
            """{"type":"ModifiedComponent","props":{"modifier":{"type":"frame","props":{"width":200,"height":90,"children":[]}},"content":[{"type":"List","props":{"children":[{"type":"Text","props":{"rawValue":"Listed row","children":[]}}]}}]}}"""
        )
        compose.onNodeWithText("Listed row").assertIsDisplayed()
    }
}
