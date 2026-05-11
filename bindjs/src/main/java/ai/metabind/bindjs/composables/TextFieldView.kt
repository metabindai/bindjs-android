package ai.metabind.bindjs.composables

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
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
    TextField(
        onValueChange = { newText ->
            component.props.setTextId?.let {
                onUiEvent(
                    UiEvent.OnTap(
                        it
                    )
                )
            }
        },
        modifier = modifiers.buildModifier(onUiEvent),
        value = component.props.text ?: "",
        placeholder = { Text(text = component.props.placeholder ?: "") })
}
