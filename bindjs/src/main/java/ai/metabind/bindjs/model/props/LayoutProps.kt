package ai.metabind.bindjs.model.props

import androidx.compose.ui.Alignment
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Props
import ai.metabind.bindjs.model.ext.toAlignment
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

class LayoutProps(
    val spacing: Float?,
    val alignment: String?,
    /**
     * SwiftUI's `pinnedViews:` on a lazy stack. Kept as a raw [JsonElement] because a
     * component is free to write the SwiftUI-shaped `pinnedViews: ['sectionHeaders']`,
     * and an array arriving at a `String` field fails the whole tree parse rather than
     * just this prop. Read it through [pinnedViews].
     */
    @SerializedName("pinnedViews")
    private val _pinnedViews: JsonElement? = null,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    /**
     * The pinning the stack asked for, or null for none.
     *
     * Only the string forms (`'sectionHeaders'`, `'sectionFooters'`, `'all'`) count, which
     * is what bindjs-apple honours: its `directive["pinnedViews"]` funnels the prop through
     * `PinnedScrollableViews(rawValue: String(describing: value))`, so an array stringifies
     * to `["sectionHeaders"]`, matches nothing and silently becomes `[]`. Reading the array
     * form here would pin on Android and not on iOS, so it is left inert on both until
     * bindjs-runtime and bindjs-apple agree on the array shape.
     */
    val pinnedViews: PinnedViews?
        get() {
            val raw = _pinnedViews?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString ?: return null
            return when (raw) {
                "sectionHeaders" -> PinnedViews.SECTION_HEADERS
                "sectionFooters" -> PinnedViews.SECTION_FOOTERS
                "all" -> PinnedViews.ALL
                else -> null
            }
        }

    override fun toString(): String {
        return "LayoutProps(spacing=$spacing, alignment=$alignment, pinnedViews=$_pinnedViews)"
    }
}

/** The subset of SwiftUI's `PinnedScrollableViews` the runtime spells out. */
enum class PinnedViews {
    SECTION_HEADERS,
    SECTION_FOOTERS,
    ALL;

    val pinsHeaders: Boolean get() = this == SECTION_HEADERS || this == ALL
}

fun LayoutProps.horizontalAlignment(): Alignment.Horizontal {
    return when (alignment) {
        "leading" -> Alignment.Start
        "trailing" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
}

/**
 * How an `HStack` aligns children that carry a text baseline. SwiftUI's
 * `firstTextBaseline` / `lastTextBaseline` are not `Alignment.Vertical` values in Compose —
 * a Row expresses them per child, with `Modifier.alignBy(FirstBaseline / LastBaseline)` —
 * so [RowView] reads this and leaves [verticalAlignment] out of it.
 */
enum class BaselineAlignment { FIRST, LAST }

fun LayoutProps.baselineAlignment(): BaselineAlignment? {
    return when (alignment) {
        "firstTextBaseline" -> BaselineAlignment.FIRST
        "lastTextBaseline" -> BaselineAlignment.LAST
        else -> null
    }
}

fun LayoutProps.verticalAlignment(): Alignment.Vertical {
    return when (alignment) {
        // `leading` / `trailing` are not vertical alignments in SwiftUI — it drops them
        // back to `center`. They are kept here because Android content has been written
        // against them since before the two runtimes were compared, and removing them
        // would silently re-lay-out that content without making any iOS render better.
        "leading" -> Alignment.Top
        "top" -> Alignment.Top
        "trailing" -> Alignment.Bottom
        "bottom" -> Alignment.Bottom
        "center" -> Alignment.CenterVertically
        else -> Alignment.CenterVertically
    }
}

fun LayoutProps.uiAlignment(): Alignment {
    return alignment.toAlignment()
}
