package ai.metabind.bindjs.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
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

/** How far the whole button fades while held down, mirroring SwiftUI's press feedback. */
private const val PRESSED_ALPHA = 0.7f

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
            TintedLabel {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = if (modifier != null) listOf(modifier) else emptyList()
                )
            }
        }
    } else {
        // Deliberately not a Material `TextButton`. That gave every unstyled BindJS
        // button a pill-shaped `Surface` which *clips* the label, so a JS-side
        // `.background(...).cornerRadius(10)` came out as a capsule, plus a 58x40dp
        // minimum size and a ripple — none of which the SwiftUI renderer applies. A
        // plain tap target lets the JS-declared padding/background/corner radius be
        // exactly what shows, the way it does on iOS.
        val pressed by interactionSource.collectIsPressedAsState()
        LaunchedEffect(pressed) {
            isPressed = pressed
        }

        Box(
            modifier = Modifier
                .alpha(if (pressed) PRESSED_ALPHA else 1f)
                .then(modifiers.buildModifier(onUiEvent, exclude = listOf(FrameModifier::class)))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isEnabled,
                    role = Role.Button,
                ) {
                    onUiEvent(
                        UiEvent.OnTap(
                            component.props.handlerId
                        )
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val modifiersFinal = modifiers.modifiersToShareWithChildren()

            TintedLabel {
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
}

/**
 * Tints the label the way SwiftUI tints a `Button`'s content: text through
 * [LocalContentTint], SF-Symbol glyphs through Material's [LocalContentColor] (which
 * `ImageView` already honours). A label with its own `.foregroundStyle(...)` overrides
 * both, so this only colours what the JS left unstyled.
 */
@Composable
private fun TintedLabel(content: @Composable () -> Unit) {
    val tint = LocalAccentColor.current
    CompositionLocalProvider(
        LocalContentTint provides tint,
        LocalContentColor provides tint,
        content = content,
    )
}
