package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class SaturationModifier(
    props: SaturationProps,
) : ComponentModifier<SaturationProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val saturation = props.rawValue ?: 1f

        return Modifier.drawWithContent {
            val saturationMatrix = ColorMatrix().apply { setToSaturation(saturation) }
            val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)

            val paint = Paint().apply {
                colorFilter = saturationFilter
            }

            drawIntoCanvas { canvas ->
                val bounds = Rect(0.0f, 0.0f, size.width, size.height)
                canvas.saveLayer(bounds, paint)
                drawContent()
                canvas.restore()
            }
        }
    }
}

class SaturationProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
