package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.SpacerComponent
import ai.metabind.bindjs.model.flexibleSpacer
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * A Spacer child of a VStack: the leftover height, and at least `minLength`.
 *
 * SwiftUI's `minLength` is a floor, not a size — `Spacer(minLength: 20)` still pushes its
 * siblings apart. It used to be drawn as a fixed 20dp gap. Where the height is unbounded
 * (a ScrollView, a List row) a weight has nothing to share and comes out 0, so there the
 * Spacer is its minimum, which is also what SwiftUI does.
 *
 * [child] is the Spacer as written; when it carries modifiers (`Spacer().padding(4)`,
 * `.frame(minHeight: 10)`, a background) those are drawn filling the slot, the way the
 * proposal passes through them in SwiftUI.
 */
@Composable
internal fun ColumnScope.StackSpacer(
    jsRuntime: JsRuntime,
    child: BaseComponent<*>,
    spacer: SpacerComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val minLength = spacer.props.minLength
    if (LocalInVerticalScroll.current) {
        FixedSpacer(
            jsRuntime, child, version, modifiers, onUiEvent,
            bareSize = if (minLength != null) Modifier.height(minLength.dp) else Modifier.weight(1f),
        )
    } else {
        FlexibleSpacer(
            jsRuntime, child, version, modifiers, onUiEvent,
            axis = SpacerAxis.Vertical,
            slot = Modifier.weight(1f)
                .then(if (minLength != null) Modifier.heightIn(min = minLength.dp) else Modifier),
        )
    }
}

/**
 * The HStack twin of the VStack [StackSpacer]. [flexible] is the Row's call: beside a
 * width-greedy sibling, or one that already takes the leftover, SwiftUI collapses the
 * Spacer to its minimum instead of splitting the space with it (see RowView).
 */
@Composable
internal fun RowScope.StackSpacer(
    jsRuntime: JsRuntime,
    child: BaseComponent<*>,
    spacer: SpacerComponent,
    flexible: Boolean,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val minLength = spacer.props.minLength
    if (flexible) {
        FlexibleSpacer(
            jsRuntime, child, version, modifiers, onUiEvent,
            axis = SpacerAxis.Horizontal,
            slot = Modifier.weight(1f)
                .then(if (minLength != null) Modifier.widthIn(min = minLength.dp) else Modifier),
        )
    } else {
        FixedSpacer(
            jsRuntime, child, version, modifiers, onUiEvent,
            bareSize = if (minLength != null) Modifier.width(minLength.dp) else Modifier,
        )
    }
}

/** The axis a Spacer flexes along: the main axis of the stack it is in. */
internal enum class SpacerAxis { Vertical, Horizontal }

/**
 * Set while rendering a Spacer's own modifiers inside its flexible slot, so the leaf
 * Spacer fills the slot along the stack's axis and its modifiers (a padding, a
 * background) cover the space it takes. Only the Spacer's modifiers lie between the
 * slot and the leaf (see [flexibleSpacer]), so nothing else reads it. Everywhere else a
 * Spacer stays the size it is given. The other axis is left alone: filling it would
 * stretch an HStack to the full height it is offered.
 */
internal val LocalSpacerFillsSlot = compositionLocalOf<SpacerAxis?> { null }

/** The Spacer stretched over [slot]; its own modifiers fill the slot with it. */
@Composable
private fun FlexibleSpacer(
    jsRuntime: JsRuntime,
    child: BaseComponent<*>,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    axis: SpacerAxis,
    slot: Modifier,
) {
    if (child is SpacerComponent) {
        Spacer(modifier = slot)
    } else {
        Box(modifier = slot, propagateMinConstraints = true) {
            CompositionLocalProvider(LocalSpacerFillsSlot provides axis) {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = modifiers,
                )
            }
        }
    }
}

/**
 * A Spacer that cannot flex: a bare one is [bareSize]; one under modifiers is drawn as
 * those modifiers make it, around a Spacer of no size.
 */
@Composable
private fun FixedSpacer(
    jsRuntime: JsRuntime,
    child: BaseComponent<*>,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
    bareSize: Modifier,
) {
    if (child is SpacerComponent) {
        Spacer(modifier = bareSize)
    } else {
        BindJSView(
            jsRuntime = jsRuntime,
            component = child,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
        )
    }
}
