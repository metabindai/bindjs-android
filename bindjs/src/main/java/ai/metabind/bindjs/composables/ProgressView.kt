package ai.metabind.bindjs.composables

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.addFillWidthIfNoFrame
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.ProgressViewComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun ProgressView(
    jsRuntime: JsRuntime,
    component: ProgressViewComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val value = component.props.value
    val total = component.props.total

    value?.let {
        total?.let {
            LinearProgressIndicator(
                progress = { value / total },
                drawStopIndicator = {},
                color = Color.Green,
                gapSize = (-30).dp,
                modifier = modifiers
                    .addFillWidthIfNoFrame()
                    .buildModifier(onUiEvent)
            )
        } ?: LinearProgressIndicator(
            progress = { value },
            color = Color.Green,
            drawStopIndicator = {},
            gapSize = (-30).dp,
            modifier = modifiers
                .addFillWidthIfNoFrame()
                .buildModifier(onUiEvent)
        )
    } ?: CircularProgressIndicator(
        modifier = modifiers.buildModifier(onUiEvent),
        color = Color.Green,
    )
}
