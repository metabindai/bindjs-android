package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.addFillWidthIfNoFrame
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.SliderComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * `Slider`: the optional minimum and maximum value labels flank the track, the track
 * takes the remaining width, and every change of the thumb is reported to the JS
 * `setValue` closure.
 *
 * The `label` is not drawn; it becomes the control's content description.
 *
 * A `step` makes the slider discrete. It is handed to Material 3 as a number of stops,
 * so the track shows a tick at each one and the thumb snaps between them. M3 divides
 * the range evenly, so a step that does not divide the range is rounded to the nearest
 * count of equal stops. The value reported to JS is snapped onto those same stops and
 * back onto the range, so it never carries float noise from the track geometry.
 *
 * The thumb follows the finger through local state instead of waiting for the value to
 * come back through JS. A drag fires many changes and each one is a round trip plus a
 * re-render, so a purely controlled thumb lags, and an echo of an earlier position
 * arriving mid-drag would yank it backwards. Incoming values are adopted only when no
 * drag is in progress, so the component's state still wins once the finger lifts.
 */
@Composable
fun SliderView(
    jsRuntime: JsRuntime,
    component: SliderComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val props = component.props
    val lowerBound = props.range?.firstOrNull() ?: 0.0
    val upperBound = props.range?.lastOrNull()?.takeIf { it > lowerBound } ?: (lowerBound + 1.0)
    val incomingValue = (props.value ?: lowerBound).coerceIn(lowerBound, upperBound)

    // M3 counts the stops *between* the ends, so a 0..100 slider with a step of 5 has 19.
    val steps = props.step
        ?.takeIf { it > 0.0 }
        ?.let { (((upperBound - lowerBound) / it).roundToInt() - 1).coerceAtLeast(0) }
        ?: 0
    // The distance between two adjacent stops as M3 actually lays them out; equals the
    // requested step whenever that step divides the range.
    val effectiveStep = if (props.step == null) null else (upperBound - lowerBound) / (steps + 1)

    var sliderValue by remember { mutableFloatStateOf(incomingValue.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(incomingValue, dragging) {
        if (!dragging) sliderValue = incomingValue.toFloat()
    }

    val label = props.label
    val semanticsModifier = if (label.isNullOrEmpty()) Modifier else Modifier.semantics {
        contentDescription = label
    }

    Row(
        modifier = modifiers
            .addFillWidthIfNoFrame()
            .buildModifier(onUiEvent)
            .then(semanticsModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ValueLabel(jsRuntime, props.minimumValueLabel, version, modifiers, onUiEvent)

        val accent = LocalAccentColor.current
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                // Without a setter the binding is read-only: the thumb stays where the
                // component put it.
                val handlerId = props.setValueId ?: return@Slider
                val snapped = snap(newValue.toDouble(), lowerBound, upperBound, effectiveStep)
                dragging = true
                sliderValue = snapped.toFloat()
                onUiEvent(UiEvent.OnSliderChange(handlerId, snapped))
            },
            onValueChangeFinished = { dragging = false },
            valueRange = lowerBound.toFloat()..upperBound.toFloat(),
            steps = steps,
            enabled = modifiers.isEnabled(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
            ),
            modifier = Modifier.weight(1f),
        )

        ValueLabel(jsRuntime, props.maximumValueLabel, version, modifiers, onUiEvent)
    }
}

/**
 * Bring a float from the track back onto the values the component asked for: within
 * the bounds, and on a multiple of `step` from the lower bound when one was given, so
 * JS never sees a value the slider could not have been set to.
 */
private fun snap(value: Double, lowerBound: Double, upperBound: Double, step: Double?): Double {
    val stepped = if (step == null) value else lowerBound + round((value - lowerBound) / step) * step
    return stepped.coerceIn(lowerBound, upperBound)
}

/**
 * One of the value labels, rendered through the renderer so a `Text(...).font(...)`
 * keeps its styling, and given the Slider's own text-formatting modifiers so a
 * `.font(...)` on the Slider reaches them too.
 */
@Composable
private fun ValueLabel(
    jsRuntime: JsRuntime,
    label: BaseComponent<*>?,
    version: Int,
    sliderModifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    if (label == null) return

    BindJSView(
        jsRuntime = jsRuntime,
        component = label,
        version = version,
        onUiEvent = onUiEvent,
        modifiers = sliderModifiers.modifiersToShareWithChildren(),
    )
}
