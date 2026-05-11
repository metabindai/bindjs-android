package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.CapsuleComponent
import ai.metabind.bindjs.model.CircleComponent
import ai.metabind.bindjs.model.EllipseComponent
import ai.metabind.bindjs.model.RectangleComponent
import ai.metabind.bindjs.model.RoundedRectangleComponent

class OvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            addOval(
                Rect(
                    0f,
                    0f,
                    size.width,
                    size.height
                )
            )
        }
        return Outline.Generic(path)
    }
}

class ClipShapeModifier(
    props: ClipShapeProps,
) : ComponentModifier<ClipShapeProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val component = props.rawValue

        return if (component is CircleComponent) {
            Modifier.clip(CircleShape)
        } else if (component is EllipseComponent) {
            Modifier.clip(OvalShape())
        } else if (component is RectangleComponent) {
            Modifier.clip(RectangleShape)
        } else if (component is RoundedRectangleComponent) {
            Modifier.clip(RoundedCornerShape(size = component.props.cornerRadius?.dp ?: 10.0f.dp))
        } else if (component is CapsuleComponent) {
            Modifier.clip(RoundedCornerShape(percent = 50))
        } else {
            Modifier
        }
    }
}

class ClipShapeProps(
    val rawValue: BaseComponent<*>?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
