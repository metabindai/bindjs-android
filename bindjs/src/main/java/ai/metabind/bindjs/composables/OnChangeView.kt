package ai.metabind.bindjs.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.model.OnChangeComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * Mirrors SwiftUI `.onChange(of:)`: fires the handler only when the watched
 * value changes while mounted — never on first appearance. `version` is a
 * plain parameter (not a Compose key), so positional `remember` survives
 * rerenders and `previous` tracks the last seen value across them.
 *
 * This is the trigger several tool UIs rely on to kick off follow-up work once
 * async data lands (e.g. the interior designer regenerating its hero image
 * after product lookups resolve). Without it that work never starts on Android.
 */
@Composable
fun OnChangeView(
    jsRuntime: JsRuntime,
    component: OnChangeComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val value = component.props.value
    val handlerId = component.props.handlerId

    var previous by remember(handlerId) { mutableStateOf(value) }
    LaunchedEffect(handlerId, value) {
        if (value != previous) {
            onUiEvent(UiEvent.OnChange(handlerId, previous, value))
            previous = value
        }
    }

    component.props.children?.filterNotNull()?.forEach { child ->
        BindJSView(
            jsRuntime = jsRuntime,
            component = child,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
        )
    }
}
