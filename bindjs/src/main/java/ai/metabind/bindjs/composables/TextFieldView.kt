package ai.metabind.bindjs.composables

import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.isAutoCorrectionDisabled
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.model.SecureFieldComponent
import ai.metabind.bindjs.model.TextFieldComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun TextFieldView(
    jsRuntime: JsRuntime,
    component: TextFieldComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    PlainTextField(
        value = component.props.text ?: "",
        setTextId = component.props.setTextId,
        modifier = modifiers.buildModifier(onUiEvent),
        onUiEvent = onUiEvent,
        // SwiftUI's `TextField` is single-line unless given `axis: .vertical`, which
        // BindJS does not expose — multi-line entry is `TextEditor`.
        singleLine = true,
        enabled = modifiers.isEnabled(),
        autoCorrectEnabled = !modifiers.isAutoCorrectionDisabled(),
        placeholder = component.props.placeholder ?: "",
    )
}

@Composable
fun SecureFieldView(
    jsRuntime: JsRuntime,
    component: SecureFieldComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    PlainTextField(
        value = component.props.text ?: "",
        setTextId = component.props.setTextId,
        modifier = modifiers.buildModifier(onUiEvent),
        onUiEvent = onUiEvent,
        singleLine = true,
        enabled = modifiers.isEnabled(),
        placeholder = component.props.placeholder ?: "",
        obscured = true,
    )
}
