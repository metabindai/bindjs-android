package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class PaddingModifier(
    props: PaddingModifierProps,
) : ComponentModifier<PaddingModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        // Negative padding is not allowed offset modifier has to be used instead
        val start = props.start.coerceAtLeast(0.0f).dp
        val top = props.top.coerceAtLeast(0.0f).dp
        val end = props.end.coerceAtLeast(0.0f).dp
        val bottom = props.bottom.coerceAtLeast(0.0f).dp
        return Modifier
            .padding(
                start = start,
                top = top,
                end = end,
                bottom = bottom
            )
            .then(if (props.top < 0) Modifier.offset(y = props.top.dp) else Modifier)
            .then(if (props.bottom < 0) Modifier.offset(y = -props.bottom.dp) else Modifier)
            .then(if (props.start < 0) Modifier.offset(y = props.start.dp) else Modifier)
            .then(if (props.end < 0) Modifier.offset(y = -props.end.dp) else Modifier)
    }

    override fun toString(): String {
        return "PaddingModifier($props)"
    }
}

class PaddingModifierProps(
    @SerializedName("top")
    private val _top: Float?,
    @SerializedName("bottom")
    private val _bottom: Float?,
    val leading: Float?,
    val trailing: Float?,
    val vertical: Float?,
    val horizontal: Float?,
    val all: Float?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Float?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    val default: Float?
        get() {
            return if (_top == null && _bottom == null && leading == null && trailing == null && rawValue == null) {
                DEFAULT_VALUE
            } else {
                null
            }
        }
    val top: Float
        get() = _top ?: vertical ?: all ?: rawValue ?: default ?: 0f
    val start: Float
        get() = leading ?: horizontal ?: all ?: rawValue ?: default ?: 0f
    val bottom: Float
        get() = _bottom ?: vertical ?: all ?: rawValue ?: default ?: 0f
    val end: Float
        get() = trailing ?: horizontal ?: all ?: rawValue ?: default ?: 0f

    override fun toString(): String {
        return "start=$start, top=$top, end=$end, bottom=$bottom"
    }

    companion object {
        private const val DEFAULT_VALUE = 16f
    }
}
