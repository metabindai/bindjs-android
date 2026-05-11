package ai.metabind.bindjs.model

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

class AngularGradientComponent(
    props: AngularGradientProps,
) : BaseComponent<AngularGradientProps>(props), BrushComponent {
    @Composable
    override fun createBrush(): Brush {
        return Brush.sweepGradient(
            colors = if (props.colors.isEmpty()) {
                listOf(Color.White, Color.Black)
            } else {
                props.colors.map {
                    Color(it.color)
                }
            },
            center = Offset.Unspecified
        )
    }
}

class AngularGradientProps(
    children: List<BaseComponent<*>>?,
    val colors: List<ColorComponent>,
    val center: PointF?,
    val startAngle: Float,
    val endAngle: Float,
) : Props(children = children) {
    override fun toString(): String {
        return "AngularGradientProps(colors=$colors, center=$center, startAngle=$startAngle, endAngle=$endAngle)"
    }
}
