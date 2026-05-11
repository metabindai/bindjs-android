package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.CircleComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.LinearGradientComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun CircleView(
    jsRuntime: JsRuntime,
    component: CircleComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    hasFrame: Boolean = false
) {
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .clip(CircleShape)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .backgroundModifier(component)
    )
}

@Composable
private fun Modifier.backgroundModifier(
    component: CircleComponent,
): Modifier {
    return when (component.props.fill?.style) {
        is ColorComponent -> this.background(color = Color(component.props.fill.style.color))
        is LinearGradientComponent -> this.background(brush = component.props.fill.style.createBrush())
        else -> {
            this.background(color = Color.LightGray)
        }
    }
}