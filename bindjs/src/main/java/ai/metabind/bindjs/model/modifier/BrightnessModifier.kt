package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class BrightnessModifier(
    props: BrightnessProps,
) : ComponentModifier<BrightnessProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val brightness = props.rawValue ?: 0f

        return Modifier.graphicsLayer {
            val colorMatrix = ColorMatrix().apply {
                if (brightness < 0) {
                    // Darken: scale RGB channels (0 = black, 1 = original)
                    val scale = 1f + brightness // value is negative, so this reduces from 1 to 0
                    set(0, 0, scale)  // Red
                    set(1, 1, scale)  // Green
                    set(2, 2, scale)  // Blue
                } else {
                    // Brighten: add to RGB channels
                    val offset = brightness * 255f
                    set(0, 4, offset)  // Red offset
                    set(1, 4, offset)  // Green offset
                    set(2, 4, offset)  // Blue offset
                }
            }

            this.colorFilter = ColorFilter.colorMatrix(colorMatrix)
        }
    }
}

class BrightnessProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
