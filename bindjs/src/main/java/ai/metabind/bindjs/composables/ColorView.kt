package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun ColorView(
    jsRuntime: JsRuntime,
    component: ColorComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .background(Color(component.color))
    )
}
