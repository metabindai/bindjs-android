package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class FixedSizeModifier(
    props: FixedSizeProps,
) : ComponentModifier<FixedSizeProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        val horizontal = props.resolvedHorizontal
        val vertical = props.resolvedVertical

        return if (horizontal && vertical) {
            Modifier.wrapContentSize(unbounded = true)
        } else if (horizontal) {
            Modifier.wrapContentWidth(unbounded = true)
        } else if (vertical) {
            Modifier.wrapContentHeight(unbounded = true)
        } else {
            Modifier
        }
    }
}

class FixedSizeProps(
    val axis: String?,
    val horizontal: Boolean?,
    val vertical: Boolean?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children) {
    val resolvedHorizontal: Boolean
        get() = when (axis) {
            "horizontal", "both" -> true
            "vertical" -> false
            else -> horizontal ?: true
        }

    val resolvedVertical: Boolean
        get() = when (axis) {
            "vertical", "both" -> true
            "horizontal" -> false
            else -> vertical ?: true
        }
}
