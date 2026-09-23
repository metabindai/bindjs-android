package ai.metabind.bindjs.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.R
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColumnComponent
import ai.metabind.bindjs.model.LazyColumnComponent
import ai.metabind.bindjs.model.ScrollAxis
import ai.metabind.bindjs.model.ScrollComponent
import ai.metabind.bindjs.model.SectionComponent
import ai.metabind.bindjs.model.SectionProps
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.layoutChildren
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.props.LayoutProps

/**
 * Set to true when BindJS content is hosted inside a vertically-scrolling
 * container (e.g. a chat LazyColumn). Vertical [ScrollView]s degrade to a
 * plain [Column] in that case to avoid nesting two scroll containers, which
 * crashes with "infinity maximum height constraints".
 */
val LocalHostScrollsVertically = compositionLocalOf { false }

/**
 * Set to true while rendering inside a horizontal [ScrollView] (a LazyRow).
 * Children of a horizontal scroller see unbounded width constraints, so any
 * weight-based layout inside collapses to zero width. Containers (notably
 * [RowView]) check this and lay children out at their intrinsic size instead
 * of using [Modifier.weight] when the flag is set.
 */
val LocalInHorizontalScroll = compositionLocalOf { false }

/**
 * The vertical counterpart of [LocalInHorizontalScroll]: true while the height offered to
 * this content is unbounded — inside a vertical [ScrollView] or a List, or anywhere in a
 * host that scrolls vertically — and false again under a frame that bounds the height.
 *
 * A weighted child is 0 tall in an unbounded Column, so a stack reads this to tell a
 * `Spacer(minLength:)` that can flex from one that can only be its minimum. SwiftUI does
 * the same: in a scroll view a Spacer collapses to its minimum length.
 */
val LocalInVerticalScroll = compositionLocalOf { false }

@Composable
fun ScrollView(
    jsRuntime: JsRuntime,
    component: ScrollComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    if (component.props.axis == ScrollAxis.HORIZONTAL) {
        // A horizontal ScrollView must take a bounded width to scroll; the
        // upstream `addWrapIfNoFrame` adds wrapContentSize, which leaves
        // maxWidth unbounded and collapses LazyRow's weighted children to 0.
        // Force fillMaxWidth when no explicit frame width is set.
        val hasExplicitWidth = modifiers.any { m ->
            m is FrameModifier && (m.props.width != null || m.props.maxWidth != null)
        }
        val widthFill = if (!hasExplicitWidth) Modifier.fillMaxWidth() else Modifier
        LazyRow(
            modifier = widthFill.then(modifiers.buildModifier(onUiEvent)),
        ) {
            component.props.children.expandingForEach()?.forEach { child ->
                child?.let {
                    item {
                        // Children of a horizontal scroll get unbounded width
                        // constraints — flag it so downstream containers know
                        // not to use Modifier.weight (which would collapse to
                        // 0 width). See `LocalInHorizontalScroll`.
                        CompositionLocalProvider(LocalInHorizontalScroll provides true) {
                            BindJSView(
                                jsRuntime = jsRuntime,
                                component = child,
                                version = version,
                                onUiEvent = onUiEvent,
                                modifiers = emptyList()
                            )
                        }
                    }
                }
            }
        }
    } else if (LocalHostScrollsVertically.current) {
        Column(
            modifier = modifiers.buildModifier(onUiEvent)
        ) {
            CompositionLocalProvider(LocalInVerticalScroll provides true) {
                component.props.children.expandingForEach()?.forEach { child ->
                    child?.let {
                        BindJSView(
                            jsRuntime = jsRuntime,
                            component = child,
                            version = version,
                            onUiEvent = onUiEvent,
                            modifiers = emptyList()
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifiers
                .buildModifier(onUiEvent)
        ) {
            component.props.children.expandingForEach()?.forEach { child ->
                child?.let {
                    val pinned = (child as? LazyColumnComponent)?.props?.pinnedViews
                    if (pinned != null && pinned.pinsHeaders) {
                        pinnedSectionHeaders(
                            jsRuntime = jsRuntime,
                            stack = child,
                            version = version,
                            onUiEvent = onUiEvent,
                        )
                    } else {
                        item {
                            CompositionLocalProvider(LocalInVerticalScroll provides true) {
                                BindJSView(
                                    jsRuntime = jsRuntime,
                                    component = child,
                                    version = version,
                                    onUiEvent = onUiEvent,
                                    modifiers = emptyList()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lays a `LazyVStack(..., pinnedViews: 'sectionHeaders')` out as items of the enclosing
 * [LazyColumn] so each section header can be a `stickyHeader`.
 *
 * This is the only place pinning can happen: the header has to be an item of the scrolling
 * list itself, and everywhere else a lazy stack renders as a plain [ColumnView] nested
 * inside one item. Only a stack that asked for pinning takes this path, so nothing that
 * doesn't set `pinnedViews` changes shape.
 *
 * Each piece is rendered by [ColumnView] on a one-child copy of the stack, so a row is laid
 * out by the same code, with the same alignment, as it would be in the un-pinned stack. The
 * stack's spacing is what the arrangement would otherwise have inserted, applied as top
 * padding to every piece after the first.
 *
 * `sectionFooters` is parsed but not pinned: Compose's lazy lists have no sticky footer, and
 * faking one (an overlay that tracks the section's bounds) would be a different layout from
 * the one SwiftUI produces rather than a closer one.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.pinnedSectionHeaders(
    jsRuntime: JsRuntime,
    stack: LazyColumnComponent,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val children = stack.props.children.expandingForEach().layoutChildren().orEmpty()

    /** The stack itself, carrying one piece of its content: same spacing, same alignment. */
    fun piece(content: List<BaseComponent<*>>) =
        ColumnComponent(LayoutProps(stack.props.spacing, stack.props.alignment, null, content))

    @Composable
    fun Piece(content: List<BaseComponent<*>>, top: Dp) {
        Column(modifier = Modifier.padding(top = top)) {
            CompositionLocalProvider(LocalInVerticalScroll provides true) {
                ColumnView(
                    jsRuntime = jsRuntime,
                    component = piece(content),
                    version = version,
                    modifiers = emptyList(),
                    onUiEvent = onUiEvent,
                )
            }
        }
    }

    children.forEachIndexed { index, child ->
        @Composable
        fun gapBefore(): Dp = if (index == 0) {
            0.dp
        } else {
            stack.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing)
        }

        val header = (child as? SectionComponent)?.props?.header
        if (child is SectionComponent && header != null) {
            // SwiftUI pins the header alone and lets the rows scroll under it — and, like
            // SwiftUI, gives it no backdrop of its own.
            stickyHeader { Piece(listOf(header), top = gapBefore()) }
            item {
                // The header was this section's first row a moment ago, so the body owes
                // the gap the section's own arrangement would have left under it.
                val rows = child.props.children.orEmpty().filterNotNull()
                val body = SectionComponent(SectionProps(null, null, child.props.footer, rows))
                Piece(listOf(body), top = SectionContentSpacing)
            }
        } else if (child != null) {
            item { Piece(listOf(child), top = gapBefore()) }
        }
    }
}
