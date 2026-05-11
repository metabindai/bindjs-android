package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class OnTapModifier(
    props: OnTapModifierProps,
) : ComponentModifier<OnTapModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = {
                onUiEvent(
                    UiEvent.OnTap(
                        props.handlerId
                    )
                )
            }
        )
    }
}

class OnTapModifierProps(
    val handlerId: String,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
