package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.model.ToggleComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * `Toggle`, mirroring SwiftUI's: the label leads, the control trails, and the pair fills
 * the width it is given.
 *
 * The label used to be dropped — it was missing from the props, so Gson never decoded it
 * and every labelled toggle drew as an unexplained switch. bindjs-apple is the reference
 * for these semantics, and there `Toggle(label, isOn:)` draws it.
 */
@Composable
fun ToggleView(
    component: ToggleComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val label = component.props.label

    if (label.isNullOrEmpty()) {
        ToggleSwitch(component, onUiEvent, modifiers.buildModifier(onUiEvent), modifiers.isEnabled())

        return
    }

    Row(
        modifier = modifiers.buildModifier(onUiEvent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, modifier = Modifier.weight(1f, fill = true))

        ToggleSwitch(component, onUiEvent, Modifier, modifiers.isEnabled())
    }
}

@Composable
private fun ToggleSwitch(
    component: ToggleComponent,
    onUiEvent: (UiEvent) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    Switch(
        checked = component.props.isOn,
        enabled = enabled,
        onCheckedChange = {
            component.props.setIsOnId?.let { handlerId ->
                onUiEvent.invoke(UiEvent.OnSwitch(handlerId, !component.props.isOn))
            }
        },
        modifier = modifier,
    )
}
