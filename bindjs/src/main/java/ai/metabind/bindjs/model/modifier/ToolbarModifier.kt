package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

/**
 * `.toolbar(ToolbarItem(...))` — a `ContentModifier` on the JS side, so the items
 * arrive already materialized in `content` (a single `ToolbarItem` or a `Group`
 * of them), unlike [SheetModifier]'s lazily-called body.
 *
 * Layout-neutral on its own: the enclosing `NavigationStack` reads it back off
 * its child's modifier chain and draws the bar. A `.toolbar` with no navigation
 * stack around it renders nothing, as on iOS.
 */
class ToolbarModifier(
    props: ToolbarModifierProps,
) : ComponentModifier<ToolbarModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class ToolbarModifierProps(
    val content: BaseComponent<*>?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
