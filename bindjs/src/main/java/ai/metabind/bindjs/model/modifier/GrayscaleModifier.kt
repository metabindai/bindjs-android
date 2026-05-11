package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class GrayscaleModifier(
    props: GrayscaleProps,
) : ComponentModifier<GrayscaleProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val grayscale = props.rawValue ?: 1f

        return Modifier.graphicsLayer {
            val matrix = ColorMatrix().apply { setToSaturation(1 - grayscale) }
            colorFilter = ColorFilter.colorMatrix(matrix)
        }
    }
}

class GrayscaleProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
