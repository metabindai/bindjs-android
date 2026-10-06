package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ScaledToFillModifier(
    props: ScaledToFillProps,
) : ComponentModifier<ScaledToFillProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // SwiftUI's scaledToFill() is aspectRatio(nil, .fill).
        return Modifier.aspectRatioBox(ratio = LocalContentRatio.current, fill = true)
    }
}

class ScaledToFillProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
