package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    val hasSpacer =
        component.props.children?.any { it is SpacerComponent } ?: false
    // When a Column is a non-expanding child of a Row (InRow present, no
    // Weight), it should wrap its content width instead of filling the Row.
    // This matches SwiftUI VStack behaviour inside an HStack.
    val inRowNoWeight = modifiers.any { it is LocalModifier.InRow } &&
            modifiers.none { it is LocalModifier.Weight }
    Column(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .then(if (inRowNoWeight) Modifier else Modifier.fillMaxWidth())
            // When the Column has Spacers and is inside a bounded-height
            // context, fill the available height so weighted Spacers can
            // expand (matching SwiftUI VStack behaviour with Spacers).
            .then(if (hasFrame && hasSpacer) Modifier.fillMaxHeight() else Modifier),
        verticalArrangement = Arrangement.spacedBy(space = (component.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing))),
        horizontalAlignment = component.props.horizontalAlignment(),
    ) {
        // Only count a child as "framed" if its frame actually constrains
        // height — a frame that only sets maxWidth=Infinity says nothing about
        // height, so it shouldn't trigger weight-share among siblings (which
        // would squeeze the non-framed sibling's intrinsic content).
        val hasFramedChild =
            component.props.children?.any { child ->
                val mod = (child?.props as? ModifierProps)?.modifier as? FrameModifier
                mod != null && (mod.props.height != null || mod.props.maxHeight != null)
            } ?: false

        // When the Column is inside a bounded-height context (has a Weight
        // modifier from its parent Row) and has multiple non-spacer children
        // without explicit frames, give them equal weight so they share the
        // vertical space evenly (matching SwiftUI VStack behaviour).
        val parentHasWeight = modifiers.any { it is LocalModifier.Weight }
        val nonSpacerChildren = component.props.children?.filter { it !is SpacerComponent }
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

        component.props.children?.forEach { child ->
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
                    child is Component
                ) {
                    // In SwiftUI, VStack children wrap their content by default and
                    // are positioned by the VStack's alignment.  Layout containers
                    // (HStack, VStack, ZStack), ModifiedComponents, and custom
                    // component calls (whose size is their body's size) should not
                    // get FillMaxWidth so the Column's horizontalAlignment can
                    // center them.
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
