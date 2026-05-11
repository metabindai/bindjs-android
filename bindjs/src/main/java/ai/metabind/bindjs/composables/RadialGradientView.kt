package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.RadialGradientComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun RadialGradientView(
    jsRuntime: JsRuntime,
    component: RadialGradientComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .background(
                brush = component.createBrush()
            )
    )
}
