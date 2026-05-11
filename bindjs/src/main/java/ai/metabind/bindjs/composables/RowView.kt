package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
    Row(
        modifier = modifiers
            .buildModifier(onUiEvent, listOf(ShadowModifier::class)),
        horizontalArrangement = Arrangement.spacedBy(space = (component.props.spacing?.dp ?: dimensionResource(R.dimen.default_spacing))),
        verticalAlignment = component.props.verticalAlignment()
    ) {
        @Composable
        fun doLayout() {
            val hasSpacer = component.props.children?.firstOrNull { it is SpacerComponent } != null
            val hasChildWithFixedSize = component.props.children?.any { child ->
                child?.hasFixedSizeModifier() == true
            } == true

            // Count non-spacer children that have no explicit width.
            // When multiple such children exist and at least one is a layout
            // container (ComponentCall, VStack, HStack, ZStack), give them
            // equal weight so they share the Row space evenly (matching SwiftUI
            // HStack behaviour).  HStacks whose children are ALL simple styled
            // content (e.g. ModifiedComponent wrapping Text words) should NOT
            // distribute evenly — they should wrap content naturally.
            val nonSpacerChildren = component.props.children?.filter { it !is SpacerComponent }
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

            component.props.children?.forEach { child ->
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
                    val modifiersFinal = if (child is ModifiedComponent &&
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
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f)
                        )
                    } else if (multipleFlexibleChildren && maxWidth == null && !childHasFixedSize) {
                        modifiers.modifiersToShareWithChildren() + LocalModifier.Weight(
                            Modifier.weight(1.0f)
                        )
                    } else {
                        modifiers.modifiersToShareWithChildren() +
                                LocalModifier.InRow(Modifier)
                    }
                    child?.let {
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

        doLayout()
    }
}

