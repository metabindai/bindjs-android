package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class OffsetModifier(
    props: OffsetModifierProps,
) : ComponentModifier<OffsetModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // In SwiftUI, offset(y: .nan) causes the view to not render.
        // Hide the component when either offset value is NaN.
        if ((props.x != null && props.x.isNaN()) || (props.y != null && props.y.isNaN())) {
            return Modifier.graphicsLayer(alpha = 0f)
        }

        val x = animateDpAsState((props.x ?: 0f).dp, moveAnimation).value
        val y = animateDpAsState((props.y ?: 0f).dp, moveAnimation).value

        return Modifier.offset(
            x,
            y
        )
    }
}

class OffsetModifierProps(
    val x: Float?,
    val y: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "OffsetModifierProps(x=$x, y=$y, children=$children)"
    }
}