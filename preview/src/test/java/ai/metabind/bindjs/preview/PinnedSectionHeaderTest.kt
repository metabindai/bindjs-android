package ai.metabind.bindjs.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.model.BaseComponent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * `LazyVStack(..., pinnedViews: 'sectionHeaders')` pins its section headers.
 *
 * The screenshot tests cannot show this — they capture the list at rest, where a pinned
 * header sits exactly where an unpinned one would. So this scrolls the LazyVStack fixture
 * far enough that an unpinned header would be gone, in a viewport short enough that a
 * section cannot fit its header and its last row at once, and checks the header is still
 * on screen and flush with the top of the list.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class PinnedSectionHeaderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a pinned section header stays at the top while its rows scroll under it`() {
        val fixture = componentFixtures.first { it.name == "LazyVStack" }
        val tree = GsonProvider.get()
            .fromJson(FixtureTrees.committed(fixture), BaseComponent::class.java)

        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    BindJSView(
                        jsRuntime = FakeJsRuntime(),
                        component = tree,
                        version = 1,
                        onUiEvent = {},
                    )
                }
            }
        }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Row A3"))
        compose.waitForIdle()

        val header = compose.onNodeWithText("Section A")
        header.assertIsDisplayed()
        assertEquals(
            "'Section A' is on screen but not pinned to the top of the list",
            0f,
            header.fetchSemanticsNode().positionInRoot.y,
            1f,
        )
    }
}
