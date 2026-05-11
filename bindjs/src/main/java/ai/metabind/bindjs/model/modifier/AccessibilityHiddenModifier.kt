package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AccessibilityHiddenModifier(
    props: AccessibilityHiddenModifierProps,
) : ComponentModifier<AccessibilityHiddenModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return if (props.rawValue == true) {
            Modifier.semantics {
                hideFromAccessibility()
            }
        } else {
            Modifier
        }
    }
}

class AccessibilityHiddenModifierProps(
    val rawValue: Boolean?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
