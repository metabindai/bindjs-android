package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class TransformEffectModifier(
    props: TransformEffectProps,
) : ComponentModifier<TransformEffectProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val a = props.a ?: 1f
//        val b = props.b ?: 0f // TODO, not supported in graphicsLayer
//        val c = props.c ?: 0f // TODO, not supported in graphicsLayer
        val d = props.d ?: 1f
        val tx = props.tx ?: 0f
        val ty = props.ty ?: 0f

        return Modifier.graphicsLayer {
            translationX = tx
            translationY = ty
            scaleX = a
            scaleY = d
        }
    }
}

class TransformEffectProps(
    val a: Float?,
    val b: Float?,
    val c: Float?,
    val d: Float?,
    val tx: Float?,
    val ty: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
