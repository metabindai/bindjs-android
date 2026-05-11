package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import ai.metabind.bindjs.composables.UiEvent

class OpacityModifier(
    props: FloatModifierProps,
) : ComponentModifier<FloatModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val alpha = animateFloatAsState(props.rawValue, opacityAnimation).value
        return Modifier.alpha(alpha)
    }
}
