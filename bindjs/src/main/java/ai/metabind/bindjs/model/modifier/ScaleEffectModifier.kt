package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ScaleEffectModifier(
    props: ScaleEffectProps,
) : ComponentModifier<ScaleEffectProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        val x = animateFloatAsState((props.x ?: props.rawValue ?: 1f), scaleAnimation).value
        val y = animateFloatAsState((props.y ?: props.rawValue ?: 1f), scaleAnimation).value

        return Modifier.scale(x, y)
    }
}

class ScaleEffectProps(
    val x: Float?,
    val y: Float?,
    val rawValue: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
