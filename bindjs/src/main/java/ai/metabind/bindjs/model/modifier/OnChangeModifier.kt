package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

/**
 * SwiftUI `.onChange(of:)`. Like [OnAppearModifier], the effect is consumed at
 * the ModifiedComponent layer (see BindJSView) rather than at the leaf, so it
 * survives behind frame/overlay wrappers that strip non-text modifiers from
 * their children. `buildModifier` is a no-op for the same reason.
 */
class OnChangeModifier(
    props: OnChangeModifierProps,
) : ComponentModifier<OnChangeModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class OnChangeModifierProps(
    val value: String?,
    val handlerId: String,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
