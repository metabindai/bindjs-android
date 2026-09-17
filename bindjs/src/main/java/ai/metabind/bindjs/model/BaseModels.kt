package ai.metabind.bindjs.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.OverlayModifier
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.Serializable

abstract class BaseComponent<T : Props>(open val props: T) : Serializable {
    override fun toString(): String {
        return "${this::class.simpleName}(props=$props)"
    }

    fun calculateMaxWidth(): Float? {
        // A horizontal ScrollView greedily fills the width offered to it (it
        // scrolls its content within those bounds). Reporting it as infinite-
        // width lets a parent Row hand it the *remaining* space via weight —
        // without this it's measured at intrinsic width, and a LazyRow's
        // intrinsic width is ~0, so a side-by-side "fixed column + scrolling
        // columns" table (e.g. the quote comparison) renders the scroll area
        // empty and unscrollable.
        if (this is ScrollComponent && props.axis == ScrollAxis.HORIZONTAL) {
            return Float.POSITIVE_INFINITY
        }
        (this as? ModifiedComponent)?.let { modifiedComponent ->
            (this.props.modifier as? FrameModifier)?.let { frameModifier ->
                // Only a frame that actually constrains *width* answers the
                // question — a height-only frame says nothing about it, so keep
                // walking inward. SwiftUI stacks these as separate overloads
                // (`.frame(maxWidth: .infinity).frame(height: 192)`, the A2UI
                // Image envelope); stopping at the outer height frame reports
                // the image as intrinsic-width and a Row then hands it an equal
                // share instead of the leftover space.
                (frameModifier.props.width ?: frameModifier.props.maxWidth)?.let { return it }
            }
            return modifiedComponent.props.content?.firstOrNull()?.calculateMaxWidth()
        }
        // A ComponentCall is a transparent slot — its single child is the
        // rendered body, so propagate that child's width (finite or infinite).
        // Without this, a Row of ComponentCalls with explicit per-card widths
        // is misclassified as "all flexible" and weight-distributed.
        if (this is Component) {
            return props.children?.firstOrNull()?.calculateMaxWidth()
        }
        // Layout containers (VStack, HStack, ZStack) only propagate infinity:
        // a finite width on one child doesn't describe the container's width.
        this.props.children?.forEach { child ->
            val childMax = child?.calculateMaxWidth()
            if (childMax == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY
        }
        return null
    }

    fun calculateMaxHeight(): Float? {
        (this as? ModifiedComponent)?.let { modifiedComponent ->
            (this.props.modifier as? FrameModifier)?.let { frameModifier ->
                // `minHeight` is a known floor — for weight-distribution
                // siblings, that's enough to treat this child as having a
                // fixed contribution rather than purely flexible. Without
                // this, a `frame(minHeight: 160)` text section next to a
                // greedy image leaves both classified as flexible, and the
                // image consumes the whole column.
                return frameModifier.props.height
                    ?: frameModifier.props.maxHeight
                    ?: frameModifier.props.minHeight
            }
            return modifiedComponent.props.content?.firstOrNull()?.calculateMaxHeight()
        }
        if (this is Component) {
            return props.children?.firstOrNull()?.calculateMaxHeight()
        }
        this.props.children?.forEach { child ->
            val childMax = child?.calculateMaxHeight()
            if (childMax == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY
        }
        return null
    }
}

/**
 * Whether this component greedily expands to fill the vertical space offered
 * to it (SwiftUI's "flexible" leaves: Color, shapes, gradients). Text, images
 * and layout containers are *intrinsic* — they wrap their content — so they
 * are NOT greedy. Walks transparent wrappers (ModifiedComponent / ComponentCall
 * single child) down to the leaf, but treats an explicit `.frame(height/…)` as
 * a fixed contribution (not greedy) unless it is `maxHeight: .infinity`.
 *
 * Used by ColumnView to decide which siblings should share leftover height via
 * weight — without this, a VStack of pure Colors inside a bounded box collapses
 * to zero height (each Color resolves fillMaxSize against an infinite max).
 */
fun BaseComponent<*>.isVerticallyGreedy(): Boolean {
    return when (this) {
        is ModifiedComponent -> {
            val frame = (props.modifier as? FrameModifier)
            if (frame != null) {
                if (frame.props.maxHeight == Float.POSITIVE_INFINITY) return true
                if (frame.props.height != null || frame.props.maxHeight != null ||
                    frame.props.minHeight != null
                ) {
                    return false
                }
            }
            props.content?.firstOrNull()?.isVerticallyGreedy() ?: false
        }

        is Component -> props.children?.firstOrNull()?.isVerticallyGreedy() ?: false

        is ColorComponent,
        is RectangleComponent,
        is RoundedRectangleComponent,
        is CircleComponent,
        is EllipseComponent,
        is CapsuleComponent,
        is PathComponent,
        is LinearGradientComponent,
        is RadialGradientComponent,
        is AngularGradientComponent,
        is EllipticalGradientComponent,
            -> true

        else -> false
    }
}

/**
 * The horizontal counterpart of [isVerticallyGreedy]: whether this component expands
 * to fill the width offered to it. Same leaves (Color, shapes, gradients — they reach
 * their view with a synthetic fillMaxSize), same walk through transparent wrappers,
 * and an explicit `.frame(width/minWidth/maxWidth)` again pins it unless that frame is
 * `maxWidth: .infinity`.
 *
 * Used by ColumnView to size a *wrapping* VStack off its intrinsic children, the way
 * SwiftUI does: in `VStack { Text("Overview"); Rectangle().frame(height: 2) }` the
 * rectangle stretches to the text's width, it doesn't stretch the stack to the parent's.
 */
fun BaseComponent<*>.isHorizontallyGreedy(): Boolean {
    return when (this) {
        is ModifiedComponent -> {
            val frame = (props.modifier as? FrameModifier)
            if (frame != null) {
                if (frame.props.maxWidth == Float.POSITIVE_INFINITY) return true
                if (frame.props.width != null || frame.props.maxWidth != null ||
                    frame.props.minWidth != null
                ) {
                    return false
                }
            }
            props.content?.firstOrNull()?.isHorizontallyGreedy() ?: false
        }

        is Component -> props.children?.firstOrNull()?.isHorizontallyGreedy() ?: false

        is ColorComponent,
        is RectangleComponent,
        is RoundedRectangleComponent,
        is CircleComponent,
        is EllipseComponent,
        is CapsuleComponent,
        is PathComponent,
        is LinearGradientComponent,
        is RadialGradientComponent,
        is AngularGradientComponent,
        is EllipticalGradientComponent,
            -> true

        else -> false
    }
}

/**
 * Whether this subtree contains a visual media leaf (image, 3D model, video)
 * that may be scaled or offset to render outside its frame — e.g. a hero image
 * that intentionally spills past a fixed-height header into the content below.
 *
 * A height-only frame is auto-clipped to enforce its height (see FrameModifier),
 * which is correct for text slots but wrongly crops such overflowing media. iOS
 * never clips unless asked (an explicit `clipped`/`clipShape` modifier still
 * works), so when media is present we skip the auto-clip to match it.
 */
fun BaseComponent<*>.containsOverflowingMedia(): Boolean {
    when (this) {
        is ImageComponent, is Model3DComponent, is VideoComponent -> return true
        else -> {}
    }
    (this as? ModifiedComponent)?.let { modified ->
        // A hero header carries its background/foreground media inside a
        // background (or overlay) modifier — not in `content` — so the
        // overflowing image is reachable only through the modifier's content.
        when (val mod = modified.props.modifier) {
            is BackgroundModifier ->
                if (mod.props.content?.containsOverflowingMedia() == true) return true
            is OverlayModifier ->
                if (mod.props.content?.containsOverflowingMedia() == true) return true
            else -> {}
        }
        modified.props.content?.forEach { child ->
            if (child?.containsOverflowingMedia() == true) return true
        }
    }
    props.children?.forEach { child ->
        if (child?.containsOverflowingMedia() == true) return true
    }
    return false
}

/**
 * Whether this subtree is a leaf that fills whatever space its frame offers
 * instead of measuring an intrinsic height — shapes, colors and gradients, all
 * of which reach their renderer with a `fillMaxSize` (see `addFillIfNoFrame` in
 * BindJSView).
 *
 * A `frame(width:height:)` measures its content with an *unbounded* height so a
 * too-tall child overflows the slot the way iOS does (see FrameModifier). Fill
 * content has no height of its own to overflow with, so an unbounded proposal
 * collapses it to zero — a segmented bar built from
 * `Rectangle().frame(width: w, height: 16)` drew nothing at all. Such content
 * has to keep the bounded max-height. Modifier chains are unwrapped so a
 * `Rectangle().clipShape(...)` inside the frame is still recognised.
 */
fun BaseComponent<*>.fillsFrameHeight(): Boolean {
    when (this) {
        is RectangleComponent,
        is RoundedRectangleComponent,
        is CapsuleComponent,
        is EllipseComponent,
        is CircleComponent,
        is PathComponent,
        is ColorComponent,
        is LinearGradientComponent,
        is RadialGradientComponent,
        is AngularGradientComponent,
        is EllipticalGradientComponent,
        is PlaceholderComponent,
            -> return true

        else -> {}
    }
    (this as? ModifiedComponent)?.props?.content?.forEach { child ->
        if (child?.fillsFrameHeight() == true) return true
    }
    return false
}

/**
 * SwiftUI's `ForEach` is a transparent container: once expanded, its rows become
 * direct children of the enclosing stack, inheriting that stack's width and
 * alignment. On Android an expanded `ForEach` arrives as a single
 * [ForEachComponent] child, so splice its children into the parent's child list.
 * This lets the parent layout (Column / Row / Box / Group / Scroll) lay the rows
 * out directly — with the correct fill-width and alignment handling — instead of
 * routing them through the nested, alignment-blind `Column` in `ForEachView`
 * (which neither fills the parent width nor honours its `horizontalAlignment`).
 */
fun List<BaseComponent<*>?>?.expandingForEach(): List<BaseComponent<*>?>? {
    if (this == null) return null
    if (none { it is ForEachComponent }) return this
    return flatMap { child ->
        if (child is ForEachComponent) child.props.children ?: emptyList()
        else listOf(child)
    }
}

/**
 * Children that take part in stack layout, i.e. everything except [EmptyComponent].
 *
 * `Empty()` is how a BindJS component says "nothing here" — the else-branch of an
 * optional label, hint or badge — and SwiftUI's `EmptyView` neither draws nor occupies
 * space. It must not sway how its siblings are sized either: a Row that counts an
 * `Empty()` among its flexible children hands out weights that no longer match what is
 * actually on screen.
 */
fun List<BaseComponent<*>?>?.layoutChildren(): List<BaseComponent<*>?>? {
    if (this == null) return null
    if (none { it is EmptyComponent }) return this
    return filter { it !is EmptyComponent }
}

/**
 * The tree seen from the outside: the modifiers wrapping a node, outermost first,
 * and the node they finally wrap.
 *
 * SwiftUI hands `navigationTitle`, `toolbar` and `presentationDetents` to the
 * *container* — a navigation stack, a sheet — rather than to the view they are
 * written on, so the container has to read them back off the chain around its
 * content. Walks through `ComponentCall` slots too (a custom component's body is
 * where such a modifier usually sits), and stops at a nested `NavigationStack`,
 * whose chrome belongs to that stack.
 */
class WrappedComponent(
    val modifiers: List<ComponentModifier<*>>,
    val root: BaseComponent<*>?,
)

fun BaseComponent<*>.unwrap(): WrappedComponent {
    val modifiers = mutableListOf<ComponentModifier<*>>()
    var node: BaseComponent<*>? = this
    while (true) {
        val next = when (node) {
            is ModifiedComponent -> {
                node.props.modifier?.let { modifiers.add(it) }
                node.props.content?.firstOrNull()
            }

            is Component -> node.props.children?.firstOrNull()
            else -> return WrappedComponent(modifiers, node)
        }
        if (next is NavigationStackComponent) return WrappedComponent(modifiers, next)
        node = next
    }
}

interface BrushComponent {
    @Composable
    fun createBrush(): Brush
}

/**
 * A `ComponentCall`: the slot the runtime wraps around a user-defined component's
 * body, carrying the component's name and the props it was called with. Renders its
 * single child, unless the embedding app has registered a native composable under the
 * same name in the [ai.metabind.bindjs.composables.ComponentRegistry], in which case
 * that composable renders instead.
 */
class Component(
    val type: String,
    props: ComponentCallProps,
) : BaseComponent<ComponentCallProps>(props) {
    override fun toString(): String {
        return "Component(type=$type)"
    }
}

class ComponentCallProps(
    name: String?,
    /** The props the component was called with, as sent by the runtime. */
    val props: JsonElement?,
    children: List<BaseComponent<*>?>?,
) : Props(name = name, children = children) {
    /** [props] as a map; empty when the call carried no object props. */
    val propsMap: Map<String, Any?>
        get() = (props as? JsonObject)?.let {
            Gson().fromJson<Map<String, Any?>>(it, object : TypeToken<Map<String, Any?>>() {}.type)
        } ?: emptyMap()
}

open class Props(
    val name: String? = null,
    val children: List<BaseComponent<*>?>?,
) : Serializable {
    override fun toString(): String {
        return "Props(children=$children)"
    }
}
