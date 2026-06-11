package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.expandingForEach
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
    // In SwiftUI a ZStack sizes itself to the union of its children and lets
    // each child keep its own size; only flexible views (Color, shapes, images)
    // expand to fill. Those leaves already self-fill via ComponentInnerView's
    // addFillIfNoFrame, so we must NOT force-fill content children (VStack,
    // Group, Text) here — doing so makes the ZStack always stretch to full
    // width, which left-aligns wrapped content and makes rotationEffect pivot
    // around the parent's center instead of the content's own center.
    //
    // The exception is a ZStack the parent has already told to fill (a
    // background layer, or a maxWidth:.infinity frame): there we keep
    // stretching content children horizontally so the Box's contentAlignment
    // can still position them (e.g. bottomLeading pushes content to the bottom).
    val parentFills = modifiers.any {
        it is LocalModifier.FillMaxWidth || it is LocalModifier.FillMaxSize
    }
    Box(
        modifier = modifiers
            .buildModifier(onUiEvent),
        contentAlignment = component.props.uiAlignment(),
    ) {
        component.props.children.expandingForEach()?.forEach { child ->
            val modifiersFinal = if (parentFills) {
                modifiers.modifiersToShareWithChildren() +
                    LocalModifier.FillMaxWidth(Modifier.fillMaxWidth())
            } else {
                modifiers.modifiersToShareWithChildren()
            }
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