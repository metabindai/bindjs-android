package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.R
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.props.horizontalAlignment

@Composable
fun GroupView(
    jsRuntime: JsRuntime,
    component: GroupComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    hasFrame: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            space = (component.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing))
        ),
        horizontalAlignment = component.props.horizontalAlignment(),
    ) {
        component.props.children?.forEach { child ->
            child?.let {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = modifiers,
                    hasFrame = hasFrame
                )
            }
        }
    }
}
