package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import kotlin.math.round

class OnDragGestureModifier(
    props: OnDragGestureModifierProps,
) : ComponentModifier<OnDragGestureModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val minimumDistance = props.minimumDistance ?: 0f
        val minDragDistancePx = with(LocalDensity.current) { minimumDistance.dp.toPx() }

        return Modifier.pointerInput(minDragDistancePx) {
            val velocityTracker = VelocityTracker()
            // Accumulated translation and latest location, kept in px during the
            // gesture and converted to dp only when handed off to JS.
            var translation = Offset.Zero
            var location = Offset.Zero
            var hasBegun = false

            // `toDp()` resolves against this pointerInput scope (a Density).
            fun emit(phase: String, velocityPx: Offset) {
                onUiEvent(
                    UiEvent.OnDrag(
                        handlerId = props.handlerId,
                        state = mapOf(
                            "phase" to phase,
                            "locationInView" to xy(location.x.toDp().value, location.y.toDp().value),
                            "translation" to xy(translation.x.toDp().value, translation.y.toDp().value),
                            "velocity" to xy(velocityPx.x.toDp().value, velocityPx.y.toDp().value),
                        )
                    )
                )
            }

            detectDragGestures(
                onDragStart = { start ->
                    velocityTracker.resetTracking()
                    translation = Offset.Zero
                    location = start
                    hasBegun = false
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    translation += dragAmount
                    location = change.position
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (!hasBegun) {
                        // Stay silent until the drag clears minimumDistance, then
                        // fire the first event as `began` (SwiftUI semantics).
                        if (translation.getDistance() < minDragDistancePx) return@detectDragGestures
                        hasBegun = true
                        emit("began", Offset.Zero)
                    } else {
                        emit("changed", velocityTracker.velocity())
                    }
                },
                onDragEnd = {
                    if (hasBegun) emit("ended", velocityTracker.velocity())
                },
                onDragCancel = {
                    if (hasBegun) emit("cancelled", Offset.Zero)
                }
            )
        }
    }
}

private fun VelocityTracker.velocity(): Offset {
    val v = calculateVelocity()
    return Offset(v.x, v.y)
}

private fun xy(x: Float, y: Float): Map<String, Float> =
    mapOf("x" to round2(x), "y" to round2(y))

private fun round2(value: Float): Float = round(value * 100) / 100

class OnDragGestureModifierProps(
    val handlerId: String,
    val minimumDistance: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
