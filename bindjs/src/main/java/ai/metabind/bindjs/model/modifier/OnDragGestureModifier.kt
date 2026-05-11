package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class OnDragGestureModifier(
    props: OnDragGestureModifierProps,
) : ComponentModifier<OnDragGestureModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        var offset by remember { mutableStateOf(Offset.Zero) }

        val minimumDistance = props.minimumDistance ?: 0f
        val minDragDistance = with(LocalDensity.current) { minimumDistance.dp.toPx() }

        return Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                val totalDistance = (offset + dragAmount).getDistance()
                if (totalDistance >= minDragDistance) {
                    change.consume()
                    offset += dragAmount
                    onUiEvent(
                        UiEvent.OnDrag(
                            props.handlerId
                        )
                    )
                }
            }
        }
    }
}

class OnDragGestureModifierProps(
    val handlerId: String,
    val minimumDistance: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
