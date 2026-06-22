package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
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
    val capsuleShape = RoundedCornerShape(percent = 50)
    val stroke = component.props.stroke

    // SwiftUI semantics:
    //  - `Capsule().fill(x)` / a bare `Capsule()` is a *filled* shape (a bare
    //     one inherits the foreground style, defaulting to black/primary).
    //  - `Capsule().stroke(style, lineWidth:)` is an *outline* — transparent
    //     fill, just a border. Used here as an `.overlay` to draw a hairline
    //     around a pill; rendering it as a solid fill (the old default) painted
    //     a black blob over the content below it.
    // Pick the fill source: an explicit `fill` wins; otherwise a non-stroked
    // capsule falls back to its foreground style. A stroked capsule with no
    // explicit fill stays transparent so the border is all that's drawn.
    val foregroundStyleComponent = modifiers.getForegroundStyleModifierComponent()
    val explicitFill = component.props.fill?.style
    val fillStyle: Any? = when {
        explicitFill is ColorComponent || explicitFill is BrushComponent -> explicitFill
        stroke != null -> null
        else -> foregroundStyleComponent
    }

    var boxModifier = modifiers
        .buildModifier(onUiEvent)
        .clip(capsuleShape)

    boxModifier = when (fillStyle) {
        is ColorComponent ->
            if (fillStyle.isMaterial()) boxModifier
            else boxModifier.background(color = Color(fillStyle.color), shape = capsuleShape)

        is BrushComponent ->
            boxModifier.background(brush = fillStyle.createBrush(), shape = capsuleShape)

        // No fill resolved: a bare Capsule fills black (matches the prior
        // default), but a stroke-only Capsule must stay transparent.
        else ->
            if (stroke == null) boxModifier.background(color = Color.Black, shape = capsuleShape)
            else boxModifier
    }

    if (stroke != null) {
        val strokeWidth = (stroke.width ?: 1f).dp
        boxModifier = when (val strokeStyle = stroke.style) {
            is ColorComponent ->
                boxModifier.border(width = strokeWidth, color = Color(strokeStyle.color), shape = capsuleShape)

            is BrushComponent ->
                boxModifier.border(width = strokeWidth, brush = strokeStyle.createBrush(), shape = capsuleShape)

            else -> boxModifier
        }
    }

    Box(modifier = boxModifier)
}
