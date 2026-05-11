package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getForegroundColor
import ai.metabind.bindjs.composables.ext.getForegroundStyleModifierComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.RoundedRectangleComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun RoundedRectangleView(
    jsRuntime: JsRuntime,
    component: RoundedRectangleComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val foregroundStyleComponent = modifiers.getForegroundStyleModifierComponent()

    when (foregroundStyleComponent) {
        is ColorComponent -> {
            val color = foregroundStyleComponent.getForegroundColor()

            Box(
                modifier = modifiers
                    .buildModifier(onUiEvent)
                    .clip(RoundedCornerShape(component.props.cornerRadius?.dp ?: 10.0f.dp))
                    .background(
                        if (component.props.fill != null &&
                            component.props.fill.style is ColorComponent
                        ) {
                            Color(component.props.fill.style.color)
                        } else {
                            color
                        }
                    )
            )
        }

        is BrushComponent -> {
            val brush = foregroundStyleComponent.createBrush()

            Box(
                modifier = modifiers
                    .buildModifier(onUiEvent)
                    .clip(RoundedCornerShape(component.props.cornerRadius?.dp ?: 10.0f.dp))
                    .background(
                        brush = brush
                    )
            )
        }

        else -> {
            val color = modifiers.getForegroundColor()

            Box(
                modifier = modifiers
                    .buildModifier(onUiEvent)
                    .clip(RoundedCornerShape(component.props.cornerRadius?.dp ?: 10.0f.dp))
                    .background(
                        color = if (component.props.fill != null &&
                            component.props.fill.style is ColorComponent
                        ) {
                            Color(component.props.fill.style.color)
                        } else {
                            color
                        }
                    )
            )
        }
    }
}
