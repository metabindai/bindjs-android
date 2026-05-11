package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ResizableModifier(
    props: ResizableModifierProps,
) : ComponentModifier<ResizableModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class ResizableModifierProps(
    children: List<BaseComponent<*>>? = null,
) : ComponentModifierProps(children)
