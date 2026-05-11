package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.ScrollAxis
import ai.metabind.bindjs.model.ScrollComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * Set to true when BindJS content is hosted inside a vertically-scrolling
 * container (e.g. a chat LazyColumn). Vertical [ScrollView]s degrade to a
 * plain [Column] in that case to avoid nesting two scroll containers, which
 * crashes with "infinity maximum height constraints".
 */
val LocalHostScrollsVertically = compositionLocalOf { false }

@Composable
fun ScrollView(
    jsRuntime: JsRuntime,
    component: ScrollComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    if (component.props.axis == ScrollAxis.HORIZONTAL) {
        LazyRow(
            modifier = modifiers
                .buildModifier(onUiEvent),
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
