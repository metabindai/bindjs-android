package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class LayoutPriorityModifier(
    props: LayoutPriorityProps,
) : ComponentModifier<LayoutPriorityProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class LayoutPriorityProps(
    val rawValue: Double = 0.0,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
