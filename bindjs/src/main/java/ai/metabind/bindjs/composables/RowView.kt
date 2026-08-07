package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.R
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.hasFixedSizeModifier
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.ColumnComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.RowComponent
import ai.metabind.bindjs.model.SpacerComponent
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.layoutChildren
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LayoutPriorityModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.ShadowModifier
import ai.metabind.bindjs.model.props.verticalAlignment

@Composable
fun RowView(
    jsRuntime: JsRuntime,
    component: RowComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    // A Row inside a horizontal LazyRow is measured with unbounded width.
    // Modifier.weight against an infinite max collapses to 0, so any branch
    // below that wraps a child in `Modifier.weight(...)` produces 0-width
    // children. When this flag is set, fall through to the intrinsic-size
    // branch (LocalModifier.InRow) for every child. Children inside the row
    // themselves are not inside a horizontal scroll (the row bounds them),
    // so reset the flag for the BindJSView call so nested Rows still get
    // weight distribution where appropriate.
    val inHorizontalScroll = LocalInHorizontalScroll.current
    // Splice any ForEach columns in as direct children so this Row lays them out
    // (weight distribution / spacers / alignment) like SwiftUI's transparent ForEach.
    val children = component.props.children.expandingForEach().layoutChildren()
    // A horizontally-greedy child (e.g. a nested horizontal ScrollView, reported
    // as infinite-width) is laid out with Modifier.weight below — but weight
    // resolves to 0 against an unbounded Row. A plain HStack wraps its content
    // width, so fill the row width when such a child is present so the weight has
    // real space to distribute (the "fixed column + scrolling columns" table).
    // Skip inside a horizontal scroll, where width is intentionally unbounded.
    val hasGreedyChild = !inHorizontalScroll &&
            (children?.any { it?.calculateMaxWidth() == Float.POSITIVE_INFINITY } ?: false)
    Row(
        modifier = modifiers
            .buildModifier(onUiEvent, listOf(ShadowModifier::class))
            .then(if (hasGreedyChild) Modifier.fillMaxWidth() else Modifier),
        horizontalArrangement = Arrangement.spacedBy(space = (component.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing))),
        verticalAlignment = component.props.verticalAlignment()
    ) {
        @Composable
        fun doLayout() {
            val hasSpacer = children?.firstOrNull { it is SpacerComponent } != null
            val hasChildWithFixedSize = children?.any { child ->
                child?.hasFixedSizeModifier() == true
            } == true

            // Count non-spacer children that have no explicit width.
            // When multiple such children exist and at least one is a layout
            // container (ComponentCall, VStack, HStack, ZStack), cap each at an
            // equal share of the Row so one wide child can't measure at the full
            // width and starve its siblings (Compose offers an unweighted child
            // all the remaining space; SwiftUI's HStack splits it). The cap is
            // `fill = false`, so a child still reports its own width and the Row
            // packs them at the leading edge — an HStack is content-sized in
            // SwiftUI, and stretching each child to exactly 1/N turns a
            // `Row [Text("FROM"), Text(email)]` into evenly-spread columns.
            // HStacks whose children are ALL simple styled content (e.g.
            // ModifiedComponent wrapping Text words) skip this entirely.
            val nonSpacerChildren = children?.filter { it !is SpacerComponent }
            val hasLayoutContainerChild = nonSpacerChildren?.any { child ->
                child is Component || child is ColumnComponent ||
                        child is RowComponent || child is BoxComponent
            } == true
            val childrenWithoutExplicitWidth = nonSpacerChildren?.count { child ->
                child?.calculateMaxWidth() == null && child?.hasFixedSizeModifier() != true
            } ?: 0
            val multipleFlexibleChildren = !hasSpacer &&
                    !hasChildWithFixedSize &&
                    hasLayoutContainerChild &&
                    nonSpacerChildren.size > 1 &&
                    childrenWithoutExplicitWidth > 1

            children?.forEach { child ->
                if (child is SpacerComponent) {
                    Spacer(
                        modifier = Modifier.then(
                            if (child.props.minLength != null) Modifier.width(
                                child.props.minLength.dp
                            ) else Modifier.weight(1.0f)
                        )
                    )
                } else {
                    val maxWidth = child?.calculateMaxWidth()

                    val childHasFixedSize = child?.hasFixedSizeModifier() == true
                    val modifiersFinal = if (inHorizontalScroll) {
                        // Unbounded width: weight collapses to 0. Lay out at
                        // intrinsic size and let the LazyRow handle scrolling.
                        modifiers.modifiersToShareWithChildren() +
                                LocalModifier.InRow(Modifier)
                    } else if (child is ModifiedComponent &&
                        child.props.modifier != null &&
                        child.props.modifier is LayoutPriorityModifier
                    ) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(
                                child.props.modifier.props.rawValue.toFloat()
                            )
                        )
                    } else if (!hasSpacer && hasChildWithFixedSize && childHasFixedSize) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f, fill = false)
                        )
                    } else if (!hasSpacer && hasChildWithFixedSize) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f)
                        )
                    } else if (maxWidth == Float.POSITIVE_INFINITY) {
                        // FillMaxWidth alongside Weight so NonModifiedComponent
                        // doesn't wrap this child in wrapContentSize (which it
                        // does when no fill modifier is present) — that would
                        // offer an unbounded width to a greedy child like a
                        // horizontal ScrollView and collapse it to 0. With fill,
                        // the weighted box fills its allotted slot.
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f)
                        ) + LocalModifier.FillMaxWidth(
                            Modifier.fillMaxWidth()
                        )
                    } else if (multipleFlexibleChildren && maxWidth == null && !childHasFixedSize) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f, fill = false)
                        )
                    } else {
                        modifiers.modifiersToShareWithChildren() +
                                LocalModifier.InRow(Modifier)
                    }
                    child?.let {
                        // The Row itself bounds its children's widths, so
                        // descendants don't need the horizontal-scroll
                        // workaround. Clear the flag for the subtree.
                        CompositionLocalProvider(LocalInHorizontalScroll provides false) {
                            BindJSView(
                                jsRuntime = jsRuntime,
                                component = child,
                                version = version,
                                onUiEvent = onUiEvent,
                                modifiers = modifiersFinal
                            )
                        }
                    }
                }
            }
        }

        doLayout()
    }
}

