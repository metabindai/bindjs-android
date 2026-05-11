package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.shadow
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColorComponent

class ShadowModifier(
    props: ShadowModifierProps,
) : ComponentModifier<ShadowModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.shadow(
            offsetX = (props.x ?: 0f).dp,
            offsetY = (props.y ?: 10f).dp,
            shape = RoundedCornerShape(props.radius.dp),
            color = Color(props.color?.color ?: ColorUtils.setAlphaComponent(android.graphics.Color.BLACK, 77)),
            blurRadius = props.radius.dp
        )
    }
}

class ShadowModifierProps(
    val color: ColorComponent?,
    @SerializedName("radius")
    private val _radius: Float?,
    val rawValue: Float?,
    val x: Float?,
    val y: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    val radius: Float
        get() = _radius ?: rawValue ?: 10f
}
