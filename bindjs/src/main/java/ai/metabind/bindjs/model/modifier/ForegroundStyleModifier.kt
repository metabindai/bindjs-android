package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.materialBlur
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColorProps
import ai.metabind.bindjs.model.Component

class ForegroundStyleModifier(
    props: ForegroundStyleProps,
) : ComponentModifier<ForegroundStyleProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        val component = props.rawValue.asColorComponent()

        return if (component != null) {
            if (component.isMaterial()) {
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
    val rawValue: Any? = null,
    val color: String? = null,
    val by: Any? = null,
) : ComponentModifierProps(children)

fun Any?.asColorComponent(): ColorComponent? {
    return when (this) {
        is ColorComponent -> this
        is Component -> this.props.children?.firstNotNullOfOrNull { it.asColorComponent() }
        is String -> ColorComponent(ColorProps(rawValue = this))
        is Map<*, *> -> {
            val type = this["type"] as? String
            if (type == "Color") {
                val props = this["props"] as? Map<*, *> ?: return null
                ColorComponent(
                    ColorProps(
                        rawValue = props["rawValue"] as? String ?: props["value"] as? String,
                        r = (props["r"] as? Number)?.toFloat(),
                        g = (props["g"] as? Number)?.toFloat(),
                        b = (props["b"] as? Number)?.toFloat(),
                        a = (props["a"] as? Number)?.toFloat(),
                        opacity = (props["opacity"] as? Number)?.toFloat(),
                    )
                )
            } else {
                val color = this["color"] as? String
                if (color != null) ColorComponent(ColorProps(rawValue = color)) else null
            }
        }
        else -> null
    }
}
