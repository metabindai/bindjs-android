package ai.metabind.bindjs.composables

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getForegroundColor
import ai.metabind.bindjs.composables.ext.getForegroundStyleModifierComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.PathComponent
import ai.metabind.bindjs.model.PathElement
import ai.metabind.bindjs.model.modifier.ComponentModifier
import kotlin.math.abs

/**
 * SwiftUI's `Path`, mirroring `Path.swift` in bindjs-apple.
 *
 * A `Path` draws in its own absolute coordinate space and has no intrinsic
 * size of its own — it takes whatever space it is offered — so the dispatch in
 * BindJSView hands it a `fillMaxSize` when no frame constrains it, like the
 * other shape leaves.
 */
@Composable
fun PathView(
    jsRuntime: JsRuntime,
    component: PathComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val elements = component.props.elements.orEmpty()
    val stroke = component.props.stroke

    // A bare `Path()` is filled with the ambient foreground style, exactly as a
    // bare Rectangle or Capsule is. A stroke-only path must stay unfilled, or
    // the outline it was asked for arrives as a solid blob.
    val explicitFill = component.props.fill?.style
    val fillStyle: Any? = when {
        explicitFill is ColorComponent || explicitFill is BrushComponent -> explicitFill
        stroke != null -> null
        else -> modifiers.getForegroundStyleModifierComponent()
    }

    val fillBrush: Brush? = when (fillStyle) {
        is ColorComponent -> SolidColor(fillStyle.getForegroundColor())
        is BrushComponent -> fillStyle.createBrush()
        // Nothing resolved and no stroke to fall back on: SwiftUI's default
        // foreground style is the primary label colour, which the rest of this
        // renderer approximates as black.
        else -> if (stroke == null) SolidColor(Color.Black) else null
    }

    val strokeBrush: Brush? = when (val strokeStyle = stroke?.style) {
        is ColorComponent -> SolidColor(strokeStyle.getForegroundColor())
        is BrushComponent -> strokeStyle.createBrush()
        // `.stroke(2)` carries a width but no style — SwiftUI strokes it with
        // the foreground style.
        else -> if (stroke != null) SolidColor(modifiers.strokeFallbackColor()) else null
    }

    val density = LocalDensity.current
    val strokeWidthPx = stroke?.let { with(density) { (it.width ?: 1f).dp.toPx() } }

    Canvas(modifier = modifiers.buildModifier(onUiEvent)) {
        val path = buildPath(elements)
        fillBrush?.let { drawPath(path, brush = it) }
        if (strokeBrush != null && strokeWidthPx != null) {
            drawPath(path, brush = strokeBrush, style = Stroke(width = strokeWidthPx))
        }
    }
}

@Composable
private fun List<ComponentModifier<*>>.strokeFallbackColor(): Color {
    return when (val style = getForegroundStyleModifierComponent()) {
        is ColorComponent -> style.getForegroundColor()
        else -> Color.Black
    }
}

/**
 * Replays the JS builder's ops onto a Compose [Path]. Coordinates arrive as
 * SwiftUI points and are converted here — a Canvas draws in pixels, so without
 * this every path would come out roughly a third of its intended size on a
 * 3x screen.
 */
private fun DrawScope.buildPath(elements: List<PathElement>): Path {
    val path = Path()

    fun px(value: Float?): Float = (value ?: 0f).dp.toPx()

    elements.forEach { element ->
        when (element.op) {
            "move" -> path.moveTo(px(element.x), px(element.y))

            "line" -> path.lineTo(px(element.x), px(element.y))

            "quadCurve" -> path.quadraticTo(
                px(element.controlX), px(element.controlY),
                px(element.x), px(element.y)
            )

            "curve" -> path.cubicTo(
                px(element.control1X), px(element.control1Y),
                px(element.control2X), px(element.control2Y),
                px(element.x), px(element.y)
            )

            "arc" -> {
                val cx = px(element.centerX)
                val cy = px(element.centerY)
                val r = px(element.radius)
                val start = element.startAngle ?: 0f
                val end = element.endAngle ?: 0f
                val rect = Rect(Offset(cx, cy), r)
                arcSegments(start, end, element.clockwise == true).forEach { (from, sweep) ->
                    path.arcTo(
                        rect = rect,
                        startAngleDegrees = from,
                        sweepAngleDegrees = sweep,
                        // Core Graphics' `addArc` joins the current point to the arc
                        // start with a line; `forceMoveTo = true` would break the
                        // subpath instead.
                        forceMoveTo = false
                    )
                }
            }

            "rect" -> path.addRect(
                Rect(px(element.x), px(element.y), px(element.x) + px(element.width), px(element.y) + px(element.height))
            )

            "roundedRect" -> path.addRoundRect(
                RoundRect(
                    rect = Rect(
                        px(element.x),
                        px(element.y),
                        px(element.x) + px(element.width),
                        px(element.y) + px(element.height)
                    ),
                    cornerRadius = CornerRadius(px(element.cornerWidth), px(element.cornerHeight))
                )
            )

            "ellipse" -> path.addOval(
                Rect(px(element.x), px(element.y), px(element.x) + px(element.width), px(element.y) + px(element.height))
            )

            "lines" -> {
                element.points.orEmpty().forEachIndexed { index, point ->
                    if (point.size < 2) return@forEachIndexed
                    val x = px(point[0])
                    val y = px(point[1])
                    // `addLines` starts a new subpath at the first point, as
                    // Core Graphics does.
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }

            "close" -> path.close()
        }
    }

    return path
}

/**
 * The `arcTo` calls that draw a SwiftUI `addArc(center:radius:startAngle:endAngle:clockwise:)`,
 * as (startAngle, sweep) pairs.
 *
 * Usually one. A whole turn has to be split in two because `android.graphics.Path.arcTo`,
 * which Compose forwards to, takes the sweep *mod 360* — a `0 → 360` circle otherwise
 * arrives as a zero-length sweep and draws nothing at all. Two halves are exact, and the
 * second continues the subpath the first started.
 */
internal fun arcSegments(
    startAngle: Float,
    endAngle: Float,
    clockwise: Boolean,
): List<Pair<Float, Float>> {
    val sweep = arcSweepDegrees(startAngle, endAngle, clockwise)
    if (abs(sweep) < 360f) return listOf(startAngle to sweep)
    return listOf(
        startAngle to sweep / 2f,
        startAngle + sweep / 2f to sweep / 2f,
    )
}

/**
 * The sweep Compose needs for a SwiftUI `addArc(center:radius:startAngle:endAngle:clockwise:)`.
 *
 * Compose takes a start angle plus a signed sweep and treats a positive sweep as
 * visually clockwise; SwiftUI takes two absolute angles plus a flag that is named
 * for the maths convention rather than the flipped screen axis, so its
 * `clockwise: true` is the *decreasing*-angle direction and therefore a negative
 * sweep here. A half-turn arc written as `start: 0, end: 180, clockwise: true`
 * has to come out as -180, not +180, or it draws the opposite half of the circle.
 */
internal fun arcSweepDegrees(startAngle: Float, endAngle: Float, clockwise: Boolean): Float {
    val delta = endAngle - startAngle
    return when {
        // A full turn or more stays a full turn — normalising it would collapse
        // a `0 → 360` circle to a zero-length sweep that draws nothing.
        abs(delta) >= 360f -> if (clockwise) -360f else 360f
        clockwise && delta > 0f -> delta - 360f
        !clockwise && delta < 0f -> delta + 360f
        else -> delta
    }
}
