package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ext.toBlendMode

class BlendModeModifier(
    props: BlendModeProps,
) : ComponentModifier<BlendModeProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val blendMode = props.rawValue.toBlendMode()

        return Modifier.graphicsLayer {
            this.blendMode = blendMode
        }
    }
}

class BlendModeProps(
    val rawValue: String?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
