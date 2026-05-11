package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class IDModifier(
    props: IDModifierProps,
) : ComponentModifier<IDModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.testTag(props.rawValue ?: "")
    }
}

class IDModifierProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
