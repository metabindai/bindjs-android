package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.NavigationStackComponent
import ai.metabind.bindjs.model.ToolbarItemComponent
import ai.metabind.bindjs.model.ToolbarItemGroupComponent
import ai.metabind.bindjs.model.ToolbarItemProps
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.NavigationBarTitleDisplayModeModifier
import ai.metabind.bindjs.model.modifier.NavigationTitleModifier
import ai.metabind.bindjs.model.modifier.ToolbarModifier
import ai.metabind.bindjs.model.unwrap

/**
 * `NavigationStack(content)`.
 *
 * There is no stack to push onto here — `NavigationLink` destinations are the
 * embedder's business (`UiEvent.OnNavigationTap`) — so what this contributes over
 * rendering the content plainly is the **bar**: the title and toolbar items that
 * SwiftUI collects from modifiers written *inside* the stack. Without it a sheet
 * built the SwiftUI way loses its title and, more importantly, its Done button.
 */
@Composable
fun NavigationStackView(
    jsRuntime: JsRuntime,
    component: NavigationStackComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    hasFrame: Boolean = false,
) {
    val children = component.props.children.expandingForEach()
    val chrome = children.orEmpty().firstNotNullOfOrNull { it?.navigationChrome() }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (chrome != null && !chrome.isEmpty) {
            NavigationBar(
                jsRuntime = jsRuntime,
                version = version,
                chrome = chrome,
                onUiEvent = onUiEvent
            )
        }
        children?.forEach { child ->
            child?.let {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = it,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = modifiers,
                    hasFrame = hasFrame
                )
            }
        }
    }
}

/** Title + toolbar items gathered from the modifiers inside a navigation stack. */
class NavigationChrome(
    val title: String?,
    val displayMode: String?,
    val leading: List<BaseComponent<*>>,
    val trailing: List<BaseComponent<*>>,
) {
    val isEmpty: Boolean
        get() = title.isNullOrBlank() && leading.isEmpty() && trailing.isEmpty()

    /** SwiftUI's default is the large title; only "inline" centers it in the bar. */
    val isInline: Boolean
        get() = displayMode == "inline"
}

/**
 * Reads the bar out of the modifier chain wrapping a stack's content. The
 * outermost `navigationTitle` / `navigationBarTitleDisplayMode` wins, matching
 * SwiftUI's last-writer-wins on a preference.
 */
private fun BaseComponent<*>.navigationChrome(): NavigationChrome? {
    var title: String? = null
    var displayMode: String? = null
    val leading = mutableListOf<BaseComponent<*>>()
    val trailing = mutableListOf<BaseComponent<*>>()

    unwrap().modifiers.forEach { modifier ->
        when (modifier) {
            is NavigationTitleModifier -> title = title ?: modifier.props.rawValue
            is NavigationBarTitleDisplayModeModifier ->
                displayMode = displayMode ?: modifier.props.rawValue

            is ToolbarModifier -> modifier.props.content?.collectToolbarItems(leading, trailing)
            else -> {}
        }
    }

    val chrome = NavigationChrome(title, displayMode, leading, trailing)
    return if (chrome.isEmpty) null else chrome
}

/**
 * Sorts a `.toolbar(...)` body into the two ends of the bar. Anything that isn't
 * a toolbar item is dropped, as on iOS, and an unplaced item follows SwiftUI's
 * `.automatic` on iPhone: trailing.
 */
private fun BaseComponent<*>.collectToolbarItems(
    leading: MutableList<BaseComponent<*>>,
    trailing: MutableList<BaseComponent<*>>,
) {
    when (this) {
        // `.toolbar([a, b])` arrives wrapped in a Group (see ContentModifier in script.js).
        is GroupComponent -> props.children?.forEach { it?.collectToolbarItems(leading, trailing) }

        // `.toolbar([ForEach(actions, a => ToolbarItem(...))])`: each row is an item.
        is ForEachComponent -> props.children?.forEach { it?.collectToolbarItems(leading, trailing) }

        is ToolbarItemComponent, is ToolbarItemGroupComponent -> {
            val itemProps = (props as? ToolbarItemProps) ?: return
            when (itemProps.placement) {
                "cancellationAction",
                "navigationBarLeading",
                "topBarLeading",
                "principal",
                    -> leading.addAll(itemProps.items)

                else -> trailing.addAll(itemProps.items)
            }
        }

        else -> {}
    }
}

/**
 * The bar itself. The title is centered against the *window*, not against the
 * space the items leave — an inline title that shifts with the width of a Done
 * button reads as a layout bug — so the items sit in a Row and the title is a
 * sibling centered in the same Box.
 */
@Composable
private fun NavigationBar(
    jsRuntime: JsRuntime,
    version: Int,
    chrome: NavigationChrome,
    onUiEvent: (UiEvent) -> Unit,
) {
    val title = chrome.title?.takeIf { it.isNotBlank() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (title != null && chrome.isInline) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 72.dp),
                    color = LocalContentColor.current,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarItems(jsRuntime, version, chrome.leading, onUiEvent)
                Spacer(modifier = Modifier.weight(1f))
                ToolbarItems(jsRuntime, version, chrome.trailing, onUiEvent)
            }
        }
        if (title != null && !chrome.isInline) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                color = LocalContentColor.current,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToolbarItems(
    jsRuntime: JsRuntime,
    version: Int,
    items: List<BaseComponent<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = item,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = emptyList()
            )
        }
    }
}
