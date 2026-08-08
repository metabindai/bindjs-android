package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.R
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.ColumnComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.ModifierProps
import ai.metabind.bindjs.model.RowComponent
import ai.metabind.bindjs.model.SpacerComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.layoutChildren
import ai.metabind.bindjs.model.isHorizontallyGreedy
import ai.metabind.bindjs.model.isVerticallyGreedy
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.LayoutPriorityModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.props.horizontalAlignment

@Composable
fun ColumnView(
    jsRuntime: JsRuntime,
    component: ColumnComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    hasFrame: Boolean = false,
) {
    // Splice any ForEach rows in as direct children so this Column lays them out
    // (and centers / fills them) exactly like SwiftUI's transparent ForEach.
    val children = component.props.children.expandingForEach().layoutChildren()
    val hasSpacer =
        children?.any { it is SpacerComponent } ?: false
    // When a Column is a non-expanding child of a Row (InRow present, no
    // Weight), it should wrap its content width instead of filling the Row.
    // This matches SwiftUI VStack behaviour inside an HStack.
    val inRowNoWeight = modifiers.any { it is LocalModifier.InRow } &&
            modifiers.none { it is LocalModifier.Weight }
    // Greedy children (Color/shapes/gradients) expand to fill offered height.
    // In a bounded-height column they must share the leftover space via weight,
    // otherwise they resolve fillMaxSize against an infinite max and collapse to
    // zero height (e.g. a VStack of color swatches in an overlay).
    val hasGreedyChild = children?.any { it?.isVerticallyGreedy() == true } ?: false
    // A width-greedy leaf (a shape/color — it reaches its view with a synthetic
    // fillMaxSize) resolves that fill against the *incoming* max width, so in a
    // wrapping Column it drags the whole stack out to the parent's width. SwiftUI
    // instead sizes the stack off its intrinsic children and stretches the shape to
    // that: an A2UI tab button is `VStack { Text(title); Rectangle().frame(height: 2) }`,
    // and without this the first tab's underline claimed the entire tab bar, leaving
    // its siblings at zero width (their titles then wrapped one letter per line).
    // IntrinsicSize.Max reproduces SwiftUI's rule — the fill child reports 0 intrinsic
    // width, so the widest real child sets the stack's width.
    val wrapsToIntrinsicWidth = inRowNoWeight &&
            (children?.any { it?.isHorizontallyGreedy() == true } ?: false)
    Column(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .then(if (inRowNoWeight) Modifier else Modifier.fillMaxWidth())
            .then(if (wrapsToIntrinsicWidth) Modifier.width(IntrinsicSize.Max) else Modifier)
            // When the Column has Spacers (or greedy leaves) and is inside a
            // bounded-height context, fill the available height so the weighted
            // children can expand (matching SwiftUI VStack behaviour).
            .then(if (hasFrame && (hasSpacer || hasGreedyChild)) Modifier.fillMaxHeight() else Modifier),
        verticalArrangement = Arrangement.spacedBy(space = (component.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing))),
        horizontalAlignment = component.props.horizontalAlignment(),
    ) {
        // Only count a child as "framed" if its frame actually constrains
        // height — a frame that only sets maxWidth=Infinity says nothing about
        // height, so it shouldn't trigger weight-share among siblings (which
        // would squeeze the non-framed sibling's intrinsic content).
        val hasFramedChild =
            children?.any { child ->
                val mod = (child?.props as? ModifierProps)?.modifier as? FrameModifier
                mod != null && (mod.props.height != null || mod.props.maxHeight != null)
            } ?: false

        // When the Column is inside a bounded-height context (has a Weight
        // modifier from its parent Row) and has multiple non-spacer children
        // without explicit frames, give them equal weight so they share the
        // vertical space evenly (matching SwiftUI VStack behaviour).
        val parentHasWeight = modifiers.any { it is LocalModifier.Weight }
        val nonSpacerChildren = children?.filter { it !is SpacerComponent }
        val multipleFlexibleChildren = !hasSpacer &&
                !hasFramedChild &&
                parentHasWeight &&
                (nonSpacerChildren?.size ?: 0) > 1

        // Walks nested ModifiedComponent chains (not just the top modifier)
        // for an explicit height — so a child wrapped in padding/cornerRadius
        // around a `.frame(height: …)` is still recognised as fixed-height.
        val childHeights = nonSpacerChildren?.map { it?.calculateMaxHeight() } ?: emptyList()
        val hasFixedHeightSibling = childHeights.any { it != null && it != Float.POSITIVE_INFINITY }
        val hasFlexibleSibling = childHeights.any { it == null || it == Float.POSITIVE_INFINITY }
        // When the Column is bounded and children mix fixed-height with
        // flexible, flexible ones must absorb the remaining space via weight.
        // Without this, the fixed-height child renders at its intrinsic size
        // but flexible siblings also use intrinsic, overflowing the bound.
        val mixedFixedAndFlexible = !hasSpacer && hasFrame &&
                hasFixedHeightSibling && hasFlexibleSibling

        children?.forEach { child ->
            if (child is SpacerComponent) {
                Spacer(
                    modifier = Modifier.then(
                        if (child.props.minLength != null) Modifier.height(
                            child.props.minLength.dp
                        ) else Modifier.weight(1.0f)
                    )
                )
            } else {
                val isFramed =
                    (child?.props as? ModifierProps)?.modifier as? FrameModifier != null
                val childMaxHeight = child?.calculateMaxHeight()
                val childIsFlexible =
                    childMaxHeight == null || childMaxHeight == Float.POSITIVE_INFINITY
                val modifiersFinal = if (child is ModifiedComponent &&
                    child.props.modifier != null &&
                    child.props.modifier is LayoutPriorityModifier
                ) {
                    modifiers.modifiersToShareWithChildren() + LocalModifier.FillMaxWidth(
                        Modifier.fillMaxWidth(child.props.modifier.props.rawValue.toFloat())
                    )
                } else if (hasFrame && child?.isVerticallyGreedy() == true) {
                    // Greedy leaf in a bounded-height column: take a weighted
                    // share of the height so multiple greedy siblings (e.g.
                    // stacked color swatches) split the space evenly instead of
                    // collapsing. Runs before the generic ModifiedComponent
                    // branch below, which would otherwise leave it unsized.
                    modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                        Modifier.weight(1f)
                    ) + LocalModifier.FillMaxWidth(
                        Modifier.fillMaxWidth()
                    )
                } else if (mixedFixedAndFlexible) {
                    if (childIsFlexible) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1f)
                        )
                    } else {
                        modifiers.modifiersToShareWithChildren()
                    }
                } else if (hasFrame && isFramed) {
                    // Child with explicit frame in bounded-height Column - no weight,
                    // let its frame modifier apply the explicit size.
                    modifiers.modifiersToShareWithChildren()
                } else if (!hasSpacer && hasFrame && hasFramedChild) {
                    // Non-framed child in bounded-height Column with a framed sibling -
                    // use weight with fill=false so it uses intrinsic size.
                    modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                        Modifier.weight(1f, false)
                    )
                } else if (hasSpacer || child is ModifiedComponent ||
                    child is RowComponent || child is ColumnComponent || child is BoxComponent ||
                    child is TextComponent || child is Component
                ) {
                    // In SwiftUI, VStack children wrap their content by default and
                    // are positioned by the VStack's alignment.  Layout containers
                    // (HStack, VStack, ZStack), ModifiedComponents, Text, and custom
                    // component calls (whose size is their body's size) should not
                    // get FillMaxWidth so the Column's horizontalAlignment can
                    // center them. Force-filling a Text instead pins it top-leading
                    // (TextView wraps content at Alignment.TopStart), so a centered
                    // VStack would render its Text left-aligned. Long text still
                    // wraps to the available width since the Column bounds it.
                    modifiers.modifiersToShareWithChildren()
                } else if (multipleFlexibleChildren) {
                    modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                        Modifier.weight(1.0f)
                    ) + LocalModifier.FillMaxWidth(
                        Modifier.fillMaxWidth()
                    )
                } else {
                    modifiers.modifiersToShareWithChildren() + LocalModifier.FillMaxWidth(
                        Modifier.fillMaxWidth()
                    )
                }
                child?.let {
                    BindJSView(
                        jsRuntime = jsRuntime,
                        component = child,
                        version = version,
                        onUiEvent = onUiEvent,
                        modifiers = modifiersFinal,
                    )
                }
            }
        }
    }
}
