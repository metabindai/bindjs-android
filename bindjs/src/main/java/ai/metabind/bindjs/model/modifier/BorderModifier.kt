package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent

class BorderModifier(
    props: BorderProps,
) : ComponentModifier<BorderProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val borderComponent = props.style
        val width = props.rawValue ?: 1f

        return if (borderComponent is ColorComponent) {
            if (borderComponent.isMaterial()) {
                // TODO, support material for Border
                Modifier
            } else {
                Modifier.border(
                    width = width.dp,
                    color = Color(borderComponent.color),
                    shape = RectangleShape
                )
            }
        } else if (borderComponent is BrushComponent) {
            Modifier.border(
                width = width.dp,
                brush = borderComponent.createBrush(),
                shape = RectangleShape
            )
        } else if (borderComponent == null) {
            Modifier.border(
                width = width.dp,
                color = Color.Black,
                shape = RectangleShape
            )
        } else {
            Modifier
        }
    }
}

class BorderProps(
    children: List<BaseComponent<*>>?,
    val style: BaseComponent<*>?,
    @SerializedName(value = "rawValue", alternate = ["width"])
    val rawValue: Float?
) : ComponentModifierProps(children)
