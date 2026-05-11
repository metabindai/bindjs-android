package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AccessibilityLabelModifier(
    props: AccessibilityLabelModifierProps,
) : ComponentModifier<AccessibilityLabelModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.semantics {
            contentDescription = props.rawValue ?: ""
        }
    }
}

class AccessibilityLabelModifierProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
