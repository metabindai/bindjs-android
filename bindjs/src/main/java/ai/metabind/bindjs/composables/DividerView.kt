package ai.metabind.bindjs.composables

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.DividerComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun DividerView(
    jsRuntime: JsRuntime,
    component: DividerComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    HorizontalDivider(
        modifier = modifiers
            .buildModifier(onUiEvent)
    )
}
