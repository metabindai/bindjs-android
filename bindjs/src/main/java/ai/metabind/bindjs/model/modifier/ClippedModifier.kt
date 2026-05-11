package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ClippedModifier(
    props: ClippedProps,
) : ComponentModifier<ClippedProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.clipToBounds()
    }
}

class ClippedProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
