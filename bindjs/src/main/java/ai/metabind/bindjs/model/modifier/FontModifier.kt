package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class FontModifier(
    props: FontProps,
) : ComponentModifier<FontProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }

    override fun toString(): String {
        return "FontModifier(props=$props)"
    }
}

class FontProps(
    val rawValue: Any?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "FontProps(rawValue=$rawValue)"
    }
}
