package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent

class OpacityModifier(
    props: FloatModifierProps,
) : ComponentModifier<FloatModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val alpha = animateFloatAsState(props.rawValue, opacityAnimation).value
        // alpha == 1 is a no-op; skip the layer entirely.
        if (alpha >= 1f) return Modifier
        // Modifier.alpha (and a default graphicsLayer) composite into an
        // offscreen buffer sized to the layout bounds, which re-clips an inner
        // Unbounded blur to the rectangle (blurred gradient circles wrapped in
        // an animating .opacity(<1) render as hard blocks). ModulateAlpha
        // applies alpha per draw instruction with no offscreen buffer, so the
        // blur can still bleed past its bounds.
        return Modifier.graphicsLayer {
            this.alpha = alpha
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
    }
}
