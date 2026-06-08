package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.systemImage
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ContentUnavailableViewComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * Renders SwiftUI's `ContentUnavailableView`: a centered icon + title +
 * description (+ optional actions). Each text slot may be a nested component
 * directive or a raw string — resolved here so neither form crashes parsing.
 * A `label`, when present, overrides the icon+title pair (matches iOS).
 */
@Composable
fun ContentUnavailableView(
    jsRuntime: JsRuntime,
    component: ContentUnavailableViewComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val props = component.props
    val systemIconId = props.systemImage.systemImage()

    Column(
        modifier = modifiers.buildModifier(onUiEvent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val label = props.label
        if (label != null) {
            Slot(jsRuntime, label, version, onUiEvent, secondary = false)
        } else {
            systemIconId?.let {
                Icon(painter = painterResource(id = it), contentDescription = "")
            }
            props.title?.let { Slot(jsRuntime, it, version, onUiEvent, secondary = false) }
        }
        props.description?.let { Slot(jsRuntime, it, version, onUiEvent, secondary = true) }

        props.children?.filterNotNull()?.forEach { action ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = action,
                version = version,
                onUiEvent = onUiEvent,
            )
        }
    }
}

/**
 * A title/description slot: render a nested component directive via BindJSView,
 * or a bare string as centered text (secondary-colored for descriptions).
 */
@Composable
private fun Slot(
    jsRuntime: JsRuntime,
    element: JsonElement,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    secondary: Boolean,
) {
    if (element.isJsonObject) {
        val gson = remember { GsonProvider.get() }
        val resolved = remember(element) {
            runCatching { gson.fromJson(element, BaseComponent::class.java) }.getOrNull()
        }
        if (resolved != null) {
            BindJSView(
                jsRuntime = jsRuntime,
                component = resolved,
                version = version,
                onUiEvent = onUiEvent,
            )
        }
    } else if (element.isJsonPrimitive) {
        Text(
            text = element.asString,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = if (secondary) LocalContentColor.current.copy(alpha = 0.6f) else LocalContentColor.current,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp),
        )
    }
}
