package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getForegroundColor
import ai.metabind.bindjs.composables.ext.getForegroundStyleModifierComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.CapsuleComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun CapsuleView(
    jsRuntime: JsRuntime,
    component: CapsuleComponent,
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

            Box(
                modifier = modifiers
                    .buildModifier(onUiEvent)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color = color)
            )
        }

        is BrushComponent -> {
            val brush = foregroundStyleComponent.createBrush()

            Box(
                modifier = modifiers
                    .buildModifier(onUiEvent)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(brush = brush)
            )
        }

        else -> {
            if (component.props.fill != null &&
                component.props.fill.style is ColorComponent
            ) {
                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(color = Color(component.props.fill.style.color))
                )
            } else if (component.props.fill != null &&
                component.props.fill.style is BrushComponent
            ) {
                val brush = component.props.fill.style.createBrush()

                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(brush = brush)
                )
            } else {
                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(color = Color.Black)
                )
            }
        }
    }
}
