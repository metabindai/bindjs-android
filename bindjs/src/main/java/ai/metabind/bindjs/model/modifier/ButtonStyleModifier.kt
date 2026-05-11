package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ButtonStyleModifier(
    props: ButtonStyleProps,
) : ComponentModifier<ButtonStyleProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }
}

class ButtonStyleProps(
    val content: BaseComponent<*>?,
    val handlerId: String?,
    val props: ComponentModifierProps?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "ButtonStyleProps(content=$content, children=$children)"
    }
}
