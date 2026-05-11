package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AccessibilityValueModifier(
    props: AccessibilityValueModifierProps,
) : ComponentModifier<AccessibilityValueModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.semantics {
            stateDescription = props.rawValue ?: ""
        }
    }
}

class AccessibilityValueModifierProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
