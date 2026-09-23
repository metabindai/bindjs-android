package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.Component

/**
 * `.border(style)`, `.border({ style, width })` or `.border(width)`.
 *
 * The single-argument forms both arrive in `rawValue` — a style directive or a number —
 * so it is kept as a raw [JsonElement] and read either way. Typed as a number, a style
 * there failed the whole tree parse. bindjs-apple reads `style ?? rawValue` the same way
 * (it ignores a numeric `rawValue`; the width is kept here, as Android always has).
 */
class BorderModifier(
    props: BorderProps,
) : ComponentModifier<BorderProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val rawValue = props.rawValue
        val rawStyle = remember(rawValue) {
            rawValue?.takeIf { it.isJsonObject }?.let {
                runCatching { GsonProvider.get().fromJson(it, BaseComponent::class.java) }.getOrNull()
            }
        }
        val rawWidth = rawValue
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asFloat
        val width = props.width ?: rawWidth ?: 1f

        // A style written as a component call (`CardBorderColor()`) is its body.
        val borderComponent = when (val style = props.style ?: rawStyle) {
            is Component -> style.props.children?.firstOrNull()
            else -> style
        }

        return when (borderComponent) {
            is ColorComponent -> Modifier.border(
                width = width.dp,
                color = Color(borderComponent.color),
                shape = RectangleShape
            )

            is BrushComponent -> Modifier.border(
                width = width.dp,
                brush = borderComponent.createBrush(),
                shape = RectangleShape
            )

            null -> Modifier.border(
                width = width.dp,
                color = Color.Black,
                shape = RectangleShape
            )

            else -> Modifier
        }
    }
}

class BorderProps(
    children: List<BaseComponent<*>>?,
    val style: BaseComponent<*>?,
    val width: Float?,
    val rawValue: JsonElement?,
) : ComponentModifierProps(children)
