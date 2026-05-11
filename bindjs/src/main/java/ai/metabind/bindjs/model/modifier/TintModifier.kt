package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class TintModifier(
    props: TintModifierProps,
) : ComponentModifier<TintModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class TintModifierProps(
    val rawValue: BaseComponent<*>?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
