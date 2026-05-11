package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ColorSchemeModifier(
    props: ColorSchemeProps,
) : ComponentModifier<ColorSchemeProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // TODO
        return Modifier
    }

    override fun toString(): String {
        return "ColorSchemeModifier(${props.rawValue})"
    }
}

class ColorSchemeProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
