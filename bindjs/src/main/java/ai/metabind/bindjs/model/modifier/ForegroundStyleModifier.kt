package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColorProps
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.MaterialComponent

class ForegroundStyleModifier(
    props: ForegroundStyleProps,
) : ComponentModifier<ForegroundStyleProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        // The style is read off the chain by the leaf that draws with it (text colour,
        // a shape's fill), not applied as a Modifier of its own.
        return Modifier
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
            if (type == "Material") {
                // Typed `Any?`, so a `Material(...)` arrives as a plain Map too.
                val props = this["props"] as? Map<*, *>
                MaterialComponent(ColorProps(rawValue = props?.get("rawValue") as? String))
            } else if (type == "Color") {
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
                if (color != null) {
                    ColorComponent(ColorProps(rawValue = color))
                } else {
                    // A wrapper component (e.g. a `ComponentCall` like CardTextColor) that
                    // resolves to a color through its children. `rawValue` is typed `Any?`,
                    // so Gson hands it to us as a plain Map rather than a polymorphic
                    // Component — recurse into the nested children to find the color.
                    val props = this["props"] as? Map<*, *>
                    (props?.get("children") as? List<*>)?.firstNotNullOfOrNull { it.asColorComponent() }
                }
            }
        }
        else -> null
    }
}
