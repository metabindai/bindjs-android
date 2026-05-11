package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AccessibilityRemoveTraitsModifier(
    props: AccessibilityRemoveTraitsModifierProps,
) : ComponentModifier<AccessibilityRemoveTraitsModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.clearAndSetSemantics {
            // Empty
        }
    }
}

class AccessibilityRemoveTraitsModifierProps(
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
