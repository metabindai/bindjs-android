package ai.metabind.bindjs.composables

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.buttonStyleModifier
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ButtonComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.OnTapModifier
import ai.metabind.bindjs.model.modifier.OnTapModifierProps

@Composable
fun ButtonView(
    jsRuntime: JsRuntime,
    component: ButtonComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val isEnabled = modifiers.isEnabled()

    var childComponent by remember { mutableStateOf<BaseComponent<*>?>(null) }
    var onTapModifier by remember { mutableStateOf<OnTapModifier?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(version, isPressed) {
        modifiers.buttonStyleModifier()?.let { modifier ->
            modifier.props.handlerId?.let { handlerId ->
                childComponent =
                    jsRuntime.callButtonStyleHandler(handlerId, component.props.label, isPressed)
                onTapModifier = OnTapModifier(
                    OnTapModifierProps(
                        handlerId = component.props.handlerId, children = emptyList()
                    )
                )
            }
        }
    }

    val child = childComponent
    val modifier = onTapModifier
    if (child != null) {
        Box(
            modifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                }
            }) {
            BindJSView(
                jsRuntime = jsRuntime,
                component = child,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = if (modifier != null) listOf(modifier) else emptyList()
            )
        }
    } else {
        // Track pressed state from TextButton's interactionSource
        val textButtonPressed by interactionSource.collectIsPressedAsState()
        LaunchedEffect(textButtonPressed) {
            isPressed = textButtonPressed
        }

        TextButton(
            enabled = isEnabled,
            contentPadding = PaddingValues(0.dp),
            modifier = modifiers.buildModifier(onUiEvent, exclude = listOf(FrameModifier::class)),
            interactionSource = interactionSource,
            onClick = {
                onUiEvent(
                    UiEvent.OnTap(
                        component.props.handlerId
                    )
                )
            }) {
            val modifiersFinal = modifiers.modifiersToShareWithChildren()

            BindJSView(
                jsRuntime = jsRuntime,
                component = component.props.label,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiersFinal
            )
        }
    }
}
