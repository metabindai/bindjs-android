package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class FrameModifier(
    props: FrameModifierProps,
) : ComponentModifier<FrameModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // Treat Infinity values as "no constraint" (same as null)
        val effectiveMaxWidth = props.maxWidth?.takeIf { it.isFinite() }
        val effectiveMaxHeight = props.maxHeight?.takeIf { it.isFinite() }
        val effectiveMinWidth = props.minWidth?.takeIf { it.isFinite() }
        val effectiveMinHeight = props.minHeight?.takeIf { it.isFinite() }
        val hasMaxWidth = effectiveMaxWidth != null || (props.maxWidth != null && props.maxWidth.isInfinite())
        val hasMaxHeight = effectiveMaxHeight != null || (props.maxHeight != null && props.maxHeight.isInfinite())

        return Modifier
            .then(
                if (effectiveMinWidth != null ||
                    effectiveMaxWidth != null ||
                    effectiveMinHeight != null ||
                    effectiveMaxHeight != null
                ) {
                    Modifier.sizeIn(
                        effectiveMinWidth?.dp ?: Dp.Unspecified,
                        effectiveMinHeight?.dp ?: Dp.Unspecified,
                        effectiveMaxWidth?.dp ?: Dp.Unspecified,
                        effectiveMaxHeight?.dp ?: Dp.Unspecified
                    ).then(if (effectiveMaxWidth != null) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                    }).then(if (effectiveMaxHeight != null) {
                        Modifier.fillMaxHeight()
                    } else
                        Modifier
                    )
                } else if (hasMaxWidth || hasMaxHeight) {
                    // maxWidth/maxHeight is Infinity — just fill without sizeIn constraint
                    Modifier
                        .then(if (hasMaxWidth) Modifier.fillMaxWidth() else Modifier)
                        .then(if (hasMaxHeight) Modifier.fillMaxHeight() else Modifier)
                } else {
                    Modifier
                }
            )
            .then(
                if (props.width != null) {
                    Modifier.width(props.width.dp)
                } else {
                    Modifier
                }
            )
            .then(
                if (props.height != null) {
                    Modifier.height(props.height.dp)
                } else {
                    Modifier
                }
            )
    }

    override fun toString(): String {
        return "FrameModifier($props)"
    }
}

class FrameModifierProps(
    val width: Float? = null,
    val height: Float? = null,
    val minWidth: Float? = null,
    val maxWidth: Float? = null,
    val minHeight: Float? = null,
    val maxHeight: Float? = null,
    val alignment: String? = null,
    children: List<BaseComponent<*>?>? = null,
) : ComponentModifierProps(children) {

    override fun toString(): String {
        return "width=$width, height=$height, minWidth=$minWidth, maxWidth=$maxWidth, minHeight=$minHeight, maxHeight=$maxHeight, alignment=$alignment"
    }
}
