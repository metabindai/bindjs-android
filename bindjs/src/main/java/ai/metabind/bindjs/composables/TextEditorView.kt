package ai.metabind.bindjs.composables

import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.isAutoCorrectionDisabled
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.model.TextEditorComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun TextEditorView(
    jsRuntime: JsRuntime,
    component: TextEditorComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    PlainTextField(
        value = component.props.value,
        setTextId = component.props.setTextId,
        modifier = modifiers.buildModifier(onUiEvent),
        onUiEvent = onUiEvent,
        singleLine = false,
        enabled = modifiers.isEnabled(),
        autoCorrectEnabled = !modifiers.isAutoCorrectionDisabled(),
    )
}
