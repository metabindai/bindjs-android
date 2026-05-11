package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.materialBlur
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColorComponent

class ForegroundStyleModifier(
    props: ForegroundStyleProps,
) : ComponentModifier<ForegroundStyleProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        val component = props.rawValue

        return if (component is ColorComponent) {
            if (props.rawValue.isMaterial()) {
                Modifier.materialBlur()
            } else {
                Modifier
            }
        } else {
            Modifier
        }
    }

    override fun toString(): String {
        return "ForegroundStyleModifier(${props.rawValue})"
    }
}

class ForegroundStyleProps(
    children: List<BaseComponent<*>>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: BaseComponent<*>?,
) : ComponentModifierProps(children)
