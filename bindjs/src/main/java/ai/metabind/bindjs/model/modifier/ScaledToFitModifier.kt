package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ScaledToFitModifier(
    props: ScaledToFitProps,
) : ComponentModifier<ScaledToFitProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // SwiftUI's scaledToFit() is aspectRatio(nil, .fit).
        return Modifier.aspectRatioBox(ratio = null, fill = false)
    }
}

class ScaledToFitProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
