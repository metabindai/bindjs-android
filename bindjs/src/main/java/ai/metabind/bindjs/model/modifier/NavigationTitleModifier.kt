package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

/**
 * `.navigationTitle("…")`. Layout-neutral; the enclosing `NavigationStack`
 * reads it off its child's modifier chain, the way SwiftUI walks preferences up
 * to the bar.
 */
class NavigationTitleModifier(
    props: NavigationTitleModifierProps,
) : ComponentModifier<NavigationTitleModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class NavigationTitleModifierProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
