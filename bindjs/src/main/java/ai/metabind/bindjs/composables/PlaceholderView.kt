package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.hasFrame
import ai.metabind.bindjs.model.PlaceholderComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * `Placeholder({ name }, children)`: a slot for a native view.
 *
 * When the enclosing component call exists and [LocalComponentRegistry] has a composable
 * under `name`, that composable renders with the call's props and children. Otherwise,
 * and inside its own native composable (see [LocalResolvingPlaceholders]), the slot
 * falls back to the placeholder's own children; with none, it shows as a translucent
 * grey rounded rectangle, sized by its modifiers like any other shape.
 */
@Composable
fun PlaceholderView(
    jsRuntime: JsRuntime,
    component: PlaceholderComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val name = component.props.name
    val call = LocalComponentCall.current
    val resolving = LocalResolvingPlaceholders.current
    val native = if (call != null && name != null && name !in resolving) {
        LocalComponentRegistry.current[name]
    } else {
        null
    }

    if (call != null && name != null && native != null) {
        CompositionLocalProvider(LocalResolvingPlaceholders provides resolving + name) {
            NativeComponentView(
                jsRuntime = jsRuntime,
                component = native,
                call = call,
                version = version,
                modifiers = modifiers,
                onUiEvent = onUiEvent,
            )
        }
    } else if (!component.props.children.isNullOrEmpty()) {
        component.props.children.forEach { child ->
            if (child != null) {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = modifiers,
                )
            }
        }
    } else {
        // Like the other shapes: fill the space offered unless a frame on the slot
        // fixes its size.
        Box(
            modifier = modifiers
                .buildModifier(onUiEvent)
                .then(if (modifiers.hasFrame()) Modifier else Modifier.fillMaxSize())
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        )
    }
}
