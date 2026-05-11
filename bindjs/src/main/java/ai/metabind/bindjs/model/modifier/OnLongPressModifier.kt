package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class OnLongPressModifier(
    props: OnLongPressModifierProps,
) : ComponentModifier<OnLongPressModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val haptics = LocalHapticFeedback.current

        return Modifier.combinedClickable(
            interactionSource = null,
            indication = null,
            onClick = {
                // Empty
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onUiEvent(
                    UiEvent.OnLongPress(
                        props.handlerId
                    )
                )
            }
        )
    }
}

class OnLongPressModifierProps(
    val handlerId: String,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
