package ai.metabind.bindjs.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ai.metabind.bindjs.model.ext.offset

class LinearGradientComponent(
    props: LinearGradientProps
) : BaseComponent<LinearGradientProps>(props), BrushComponent {
    @Composable
    override fun createBrush(): Brush {
        return Brush.linearGradient(
            colors = if (props.colors.isNullOrEmpty()) {
                listOf(Color.White, Color.Black)
            } else {
                props.colors.map {
                    Color(it.color)
                }
            },
            start = props.startPoint?.offset() ?: Offset(0.5f, 0f),
            end = props.endPoint?.offset() ?: Offset(0.5f, Float.POSITIVE_INFINITY)
        )
    }
}

class LinearGradientProps(
    children: List<BaseComponent<*>>?,
    val colors: List<ColorComponent>?,
    val startPoint: String?,
    val endPoint: String?
) : Props(children = children) {
    override fun toString(): String {
        return "LinearGradientProps(colors=$colors, startPoint=$startPoint, endPoint=$endPoint)"
    }
}
