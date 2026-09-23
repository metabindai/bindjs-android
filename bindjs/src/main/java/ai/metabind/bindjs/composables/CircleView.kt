package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.min
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
    // The shape clip and the fill live on a nested child rather than on the
    // same modifier chain as the user modifiers. A `.blur(Unbounded)` coming
    // from `buildModifier()` would otherwise coalesce with the adjacent
    // `.clip(CircleShape)` graphics layer, and the clip re-bounds the blur to
    // the layout rectangle — turning soft blurred-circle glows (gradient
    // washes) into hard-edged blocks. Keeping them on separate layout nodes
    // lets the blur bleed past its bounds, matching iOS.
    //
    // SwiftUI draws a Circle as the largest circle that fits the space it is given,
    // centered in it: `Circle().frame(width: 16, height: 2416)` is a 16pt dot. This used
    // to size the circle with `aspectRatio(1f, matchHeightConstraintsFirst = true)`,
    // which first tries sizes that ignore the incoming constraints — so it took the
    // 2416 height, drew a 2416pt disc and painted over everything around it (the
    // Explore card's hotspots filled the whole card grey).
    Box(modifier = modifiers.buildModifier(onUiEvent)) {
        Box(modifier = Modifier.fillOrSquare(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .largestSquare()
                    .clip(CircleShape)
                    .backgroundModifier(component)
            )
        }
    }
}

/**
 * Takes all the space offered, as a shape does in SwiftUI. Along an unbounded axis — a
 * circle in a scroll view with no frame — it matches the bounded one instead, so the
 * circle stays round rather than collapsing to nothing.
 */
private fun Modifier.fillOrSquare(): Modifier = layout { measurable, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else null
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else null
    val placeable = measurable.measure(
        Constraints.fixed(
            constraints.constrainWidth(width ?: height ?: 0),
            constraints.constrainHeight(height ?: width ?: 0),
        )
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** The largest square that fits the space offered, which is where the circle is drawn. */
private fun Modifier.largestSquare(): Modifier = layout { measurable, constraints ->
    val side = min(constraints.maxWidth, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(side, side))
    layout(side, side) { placeable.place(0, 0) }
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