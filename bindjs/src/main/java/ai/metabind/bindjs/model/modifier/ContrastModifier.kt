package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class ContrastModifier(
    props: ContrastProps,
) : ComponentModifier<ContrastProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        val contrast = props.rawValue ?: 1f

        return Modifier.graphicsLayer {
            val offset = ((-.5f) * contrast + .5f) * 255f

            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, offset,
                    0f, contrast, 0f, 0f, offset,
                    0f, 0f, contrast, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            this.colorFilter = ColorFilter.colorMatrix(colorMatrix = colorMatrix)
        }
    }
}

class ContrastProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
