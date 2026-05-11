package ai.metabind.bindjs.composables

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
    val isEnabled = modifiers.isEnabled()
    val isAutoCorrectionDisabled = modifiers.isAutoCorrectionDisabled()

    var text by remember { mutableStateOf(component.props.rawValue ?: "") }

    TextField(
        enabled = isEnabled,
        modifier = modifiers.buildModifier(onUiEvent),
        value = text,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = !isAutoCorrectionDisabled
        ),
        onValueChange = { text = it }
    )
}
