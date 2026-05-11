package ai.metabind.bindjs.model

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

class EllipticalGradientComponent(
    props: EllipticalGradientProps
) : BaseComponent<EllipticalGradientProps>(props), BrushComponent {
    @Composable
    override fun createBrush(): Brush {
        return Brush.radialGradient(
            colors = if (props.colors.isEmpty()) {
                listOf(Color.White, Color.Black)
            } else {
                props.colors.map {
                    Color(it.color)
                }
            },
            center = Offset.Unspecified,
            radius = Float.POSITIVE_INFINITY
        )
    }
}

class EllipticalGradientProps(
    children: List<BaseComponent<*>>?,
    val colors: List<ColorComponent>,
    val center: PointF?,
    val startRadius: Float,
    val endRadius: Float
) : Props(children = children) {
    override fun toString(): String {
        return "EllipticalGradientProps(colors=$colors, center=$center, startRadius=$startRadius, endRadius=$endRadius)"
    }
}
