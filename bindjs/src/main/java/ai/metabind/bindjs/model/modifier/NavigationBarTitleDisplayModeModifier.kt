package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

/**
 * `.navigationBarTitleDisplayMode("inline" | "large" | "automatic")`. Read by the
 * enclosing `NavigationStack`; "inline" centers the title in the bar, anything
 * else drops it below the bar at display size.
 */
class NavigationBarTitleDisplayModeModifier(
    props: NavigationBarTitleDisplayModeModifierProps,
) : ComponentModifier<NavigationBarTitleDisplayModeModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class NavigationBarTitleDisplayModeModifierProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
