package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.invertColors
import ai.metabind.bindjs.model.BaseComponent

class ColorInvertModifier(
    props: ColorInvertProps,
) : ComponentModifier<ColorInvertProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.invertColors()
    }
}

class ColorInvertProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
