package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.props.uiAlignment

@Composable
fun BoxView(
    jsRuntime: JsRuntime,
    component: BoxComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    hasFrame: Boolean = false,
) {
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent),
        contentAlignment = component.props.uiAlignment(),
    ) {
        component.props.children?.forEach { child ->
            // In SwiftUI, ZStack children are proposed the stack's bounds
            // but choose their own size. Content views (VStack with text)
            // wrap their content, while fill views (Color, shapes) expand.
            // Use fillMaxWidth so children fill horizontally, but let them
            // wrap vertically so the Box's contentAlignment can position
            // them (e.g. bottomLeading alignment pushes content to the bottom).
            val modifiersFinal = modifiers.modifiersToShareWithChildren() + LocalModifier.FillMaxSize(Modifier.fillMaxWidth())
            child?.let {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = modifiersFinal,
                    hasFrame = hasFrame
                )
            }
        }
    }
}