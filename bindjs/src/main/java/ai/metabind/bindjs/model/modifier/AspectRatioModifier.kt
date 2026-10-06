package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
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
        return Modifier.aspectRatioBox(props.ratio, fill = props.contentMode == "fill")
    }
}

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
 * SwiftUI's `.aspectRatio(ratio, contentMode:)`. The content is proposed the box of [ratio]
 * that fits inside the size offered (or, with [fill], the smallest one covering it), and
 * the modifier takes the size the content answers: a flexible view, a color or a
 * GeometryReader, fills the box; a view with a size of its own keeps it; and a resizable
 * image scales to the box. Offered one length only, the box takes it and derives the
 * other; offered neither, it shapes the content's ideal size.
 *
 * A null [ratio] keeps the content's own, from its ideal size: an image's once loaded,
 * padding and frames around it included. Content without one, a color or a shape, is
 * square.
 *
 * A filled box can be larger than the space offered. It keeps its size and overflows
 * evenly on both sides, as SwiftUI's does; `.clipped()` trims it.
 */
fun Modifier.aspectRatioBox(ratio: Float?, fill: Boolean): Modifier = layout { measurable, constraints ->
    val ideal = if (ratio == null || !constraints.hasBoundedWidth && !constraints.hasBoundedHeight) measurable.idealSize() else null
    val kept = ratio ?: ideal?.let { it.width.toFloat() / it.height } ?: 1f
    val offeredWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else null
    val offeredHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else null
    val box = aspectBox(offeredWidth, offeredHeight, kept, fill)
        ?: ideal?.let { aspectBox(it.width, it.height, kept, fill) }
        ?: IntSize.Zero
    if (measurable.hasOwnSize()) {
        val placeable = measurable.measure(upTo(box))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        // The box's size, not the placeable's: when a parent asks this layout for its
        // intrinsic size, Compose measures the content as a stand-in of the content's own
        // intrinsic size, which for a color or an image still loading is nothing.
        val exact = exactly(box)
        val placeable = measurable.measure(exact)
        layout(exact.maxWidth, exact.maxHeight) { placeable.place(0, 0) }
    }
}

/**
 * Whether the content has a size no offer changes: a frame of fixed lengths, an image, a
 * stack of those. Its smallest and largest intrinsic sizes agree. Content that takes what
 * it is offered reports none (a color) or can't answer (a GeometryReader, a lazy list),
 * and Compose sizes it by the constraints alone, so it is given the box exactly.
 */
private fun IntrinsicMeasurable.hasOwnSize(): Boolean = try {
    val width = maxIntrinsicWidth(Constraints.Infinity)
    val height = maxIntrinsicHeight(Constraints.Infinity)
    width > 0 && height > 0 &&
        minIntrinsicWidth(Constraints.Infinity) == width && minIntrinsicHeight(Constraints.Infinity) == height
} catch (_: IllegalStateException) {
    false
}

/**
 * The content's ideal size, or null when it has none (a color) or can't say: a lazy list
 * answers no intrinsic measurements.
 */
private fun IntrinsicMeasurable.idealSize(): IntSize? = try {
    val width = maxIntrinsicWidth(Constraints.Infinity)
    val height = maxIntrinsicHeight(Constraints.Infinity)
    if (width > 0 && height > 0) IntSize(width, height) else null
} catch (_: IllegalStateException) {
    null
}

/** Constraints proposing [box], from nothing up to its size. */
private fun upTo(box: IntSize): Constraints = representable(box, exact = false)

/** Constraints giving the content [box] exactly. */
private fun exactly(box: IntSize): Constraints = representable(box, exact = true)

/**
 * A filled box of an extreme ratio can be longer than Compose can represent; it keeps its
 * shorter side and is cut to what Compose can measure along the longer.
 */
private fun representable(box: IntSize, exact: Boolean): Constraints {
    val minWidth = if (exact) box.width else 0
    val minHeight = if (exact) box.height else 0
    return if (box.width >= box.height) Constraints.fitPrioritizingHeight(minWidth, box.width, minHeight, box.height)
    else Constraints.fitPrioritizingWidth(minWidth, box.width, minHeight, box.height)
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
