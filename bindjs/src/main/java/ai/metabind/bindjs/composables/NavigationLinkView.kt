package ai.metabind.bindjs.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.NavigationLinkComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun NavigationLinkView(
    jsRuntime: JsRuntime,
    version: Int,
    component: NavigationLinkComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .clickable { onUiEvent.invoke(UiEvent.OnNavigationTap(component.props.destinationHandlerId)) }
    ) {
        BindJSView(
            jsRuntime = jsRuntime,
            component = component.props.label,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = emptyList()
        )
    }
}
