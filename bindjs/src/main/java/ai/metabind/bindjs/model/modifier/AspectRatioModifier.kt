package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AspectRatioModifier(
    props: AspectRatioProps,
) : ComponentModifier<AspectRatioProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.aspectRatioBox(props.ratio ?: LocalContentRatio.current, fill = props.contentMode == "fill")
    }
}

/**
 * The width over height of the content an aspect modifier wraps, when the content knows
 * it better than its intrinsic size says: an image provides its ratio once loaded. Coil
 * changes an image's intrinsic size without remeasuring the layouts that read it, so a
 * box sized from intrinsics alone kept the square it had before the image arrived.
 */
val LocalContentRatio = compositionLocalOf<Float?> { null }

class AspectRatioProps(
    // The runtime sends `aspectRatio`; older runtimes sent the ratio as `rawValue`, the
    // generic modifier key, and dropped the content mode.
    @SerializedName(value = "aspectRatio", alternate = ["rawValue"])
    private val aspectRatio: Float?,
    val contentMode: String?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children) {
    /** The ratio to keep, or null to keep the content's own. */
    val ratio: Float?
        get() = aspectRatio?.takeIf { it.isFinite() && it > 0f }
}

/**
 * SwiftUI's `.aspectRatio(ratio, contentMode:)`: the box of [ratio] that fits inside the
 * size offered (or, with [fill], the smallest one covering it), with the content
 * proposed exactly that box. Offered one length only, the box takes it and derives the
 * other; offered neither, it shapes the content's ideal size.
 *
 * A null [ratio] keeps the content's own: an image's from [LocalContentRatio], anything
 * else's from its ideal size. Content without one, a color or a shape, is square.
 *
 * A filled box can be larger than the space offered. It keeps its size and overflows
 * evenly on both sides, as SwiftUI's does; `.clipped()` trims it.
 */
fun Modifier.aspectRatioBox(ratio: Float?, fill: Boolean): Modifier = layout { measurable, constraints ->
    val kept = ratio ?: measurable.idealRatio() ?: 1f
    val offeredWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else null
    val offeredHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else null
    val box = aspectBox(offeredWidth, offeredHeight, kept, fill)
        ?: aspectBox(
            measurable.maxIntrinsicWidth(Constraints.Infinity).takeIf { it > 0 },
            measurable.maxIntrinsicHeight(Constraints.Infinity).takeIf { it > 0 },
            kept,
            fill,
        )
        ?: IntSize.Zero
    val placeable = measurable.measure(Constraints.fixed(box.width, box.height))
    layout(box.width, box.height) { placeable.place(0, 0) }
}

/** The content's width over height at its ideal size, or null when it has none. */
private fun IntrinsicMeasurable.idealRatio(): Float? {
    val width = maxIntrinsicWidth(Constraints.Infinity)
    val height = maxIntrinsicHeight(Constraints.Infinity)
    return if (width > 0 && height > 0) width.toFloat() / height else null
}

/**
 * The box of [ratio] for an offer of [width] by [height], where null means that length
 * is not offered. Null when neither is.
 */
internal fun aspectBox(width: Int?, height: Int?, ratio: Float, fill: Boolean): IntSize? = when {
    width != null && height != null -> {
        val boxWidth = if (fill) max(width.toFloat(), height * ratio) else min(width.toFloat(), height * ratio)
        IntSize(boxWidth.roundToInt(), (boxWidth / ratio).roundToInt())
    }
    width != null -> IntSize(width, (width / ratio).roundToInt())
    height != null -> IntSize((height * ratio).roundToInt(), height)
    else -> null
}
