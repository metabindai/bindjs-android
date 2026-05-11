package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class RotationEffectModifier(
    props: RotationEffectProps,
) : ComponentModifier<RotationEffectProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val rotation = animateFloatAsState(props.rawValue ?: 0f, rotationAnimation).value
        return Modifier.rotate(rotation)
    }
}

class RotationEffectProps(
    @SerializedName(value = "rawValue", alternate = ["degrees"])
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
