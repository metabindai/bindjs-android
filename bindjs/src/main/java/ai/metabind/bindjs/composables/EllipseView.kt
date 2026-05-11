package ai.metabind.bindjs.composables

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getForegroundColor
import ai.metabind.bindjs.composables.ext.getForegroundStyleModifierComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.EllipseComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun EllipseView(
    jsRuntime: JsRuntime,
    component: EllipseComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val foregroundStyleComponent = modifiers.getForegroundStyleModifierComponent()

    when (foregroundStyleComponent) {
        is ColorComponent -> {
            val color = if (component.props.fill != null &&
                component.props.fill.style is ColorComponent
            ) {
                Color(component.props.fill.style.color)
            } else {
                foregroundStyleComponent.getForegroundColor()
            }

            Canvas(modifier = modifiers.buildModifier(onUiEvent)) {
                drawOval(
                    color = color,
                    size = size
                )
            }
        }

        is BrushComponent -> {
            val brush = foregroundStyleComponent.createBrush()

            Canvas(modifier = modifiers.buildModifier(onUiEvent)) {
                drawOval(
                    brush = brush,
                    size = size
                )
            }
        }

        else -> {
            val color = if (component.props.fill != null &&
                component.props.fill.style is ColorComponent
            ) {
                Color(component.props.fill.style.color)
            } else {
                Color.Black
            }

            Canvas(modifier = modifiers.buildModifier(onUiEvent)) {
                drawOval(
                    color = color,
                    size = size
                )
            }
        }
    }
}
