package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class HiddenModifier(
    props: HiddenProps,
) : ComponentModifier<HiddenProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.alpha(0f)
    }
}

class HiddenProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
