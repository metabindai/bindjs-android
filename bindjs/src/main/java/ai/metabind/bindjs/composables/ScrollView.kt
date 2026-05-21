package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.ScrollAxis
import ai.metabind.bindjs.model.ScrollComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FrameModifier

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
            component.props.children?.forEach { child ->
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
            component.props.children?.forEach { child ->
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
    } else {
        LazyColumn(
            modifier = modifiers
                .buildModifier(onUiEvent)
        ) {
            component.props.children?.forEach { child ->
                child?.let {
                    item {
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
