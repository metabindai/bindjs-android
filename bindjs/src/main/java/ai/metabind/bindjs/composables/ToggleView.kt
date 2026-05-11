package ai.metabind.bindjs.composables

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.ToggleComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun ToggleView(
    component: ToggleComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    Switch(
        checked = component.props.isOn,
        onCheckedChange = {
            component.props.setIsOnId?.let {
                onUiEvent.invoke(UiEvent.OnSwitch(it, !component.props.isOn))
            }
        },
        modifier = modifiers
            .buildModifier(onUiEvent),
    )
}
