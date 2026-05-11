package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class OverlayModifier(
    props: OverlayModifierProps,
) : ComponentModifier<OverlayModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }

    override fun toString(): String {
        return "OverlayModifier(props=$props)"
    }
}

class OverlayModifierProps(
    val alignment: String?,
    val content: BaseComponent<*>?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "OverlayModifierProps(alignment=$alignment, content=$content, children=$children)"
    }
}