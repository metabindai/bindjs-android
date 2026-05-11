package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ZIndexModifier(
    props: ZIndexModifierProps,
) : ComponentModifier<ZIndexModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.zIndex(props.rawValue ?: 0f)
    }
}

class ZIndexModifierProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
