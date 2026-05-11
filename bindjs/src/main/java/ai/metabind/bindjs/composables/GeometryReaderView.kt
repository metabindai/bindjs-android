package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.GeometryReaderComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@Composable
fun GeometryReaderView(
    jsRuntime: JsRuntime,
    component: GeometryReaderComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit = {},
) {
    val handlerId = component.props.handlerId
    val environmentId = component.props.environmentId

    var localBounds by remember { mutableStateOf(Rect.Zero) }
    var globalBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current

    // Fallback viewport height for when constraints are unbounded (inside a
    // scroll container). iOS's GeometryProxy.size reports the viewport size
    // inside ScrollView; on Android we approximate with the View height.
    val viewHeightPx = LocalView.current.height

    BoxWithConstraints(
        modifier = modifiers
            .buildModifier(onUiEvent)
            // Use fillMaxWidth only (not fillMaxSize) so the GeometryReader
            // wraps to its content height instead of filling the parent frame.
            // This is critical because the parent frame may be an overestimate
            // (e.g. 500dp) while the actual content from the JS callback is
            // smaller (e.g. 226dp). Filling height would make sibling modifiers
            // like shadow draw at the wrong size.
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                localBounds = coordinates.boundsInParent()
                globalBounds = coordinates.boundsInWindow()
            }
    ) {
        // Dynamic children from JS handler
        var childComponent by remember { mutableStateOf<BaseComponent<*>?>(null) }

        // Derive geometry size from constraints (the proposed/available size),
        // matching iOS's GeometryProxy.size which reports the proposed size from
        // the parent, NOT the measured content size.
        //
        // Width: always bounded — use constraint max width.
        // Height: bounded when inside a frame(height:) — use that height.
        //         unbounded when inside a scroll container — fall back to
        //         the view/window height (iOS's ScrollView proposes viewport).
        val widthDp = constraints.maxWidth / density.density
        val heightDp = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) {
            constraints.maxHeight / density.density
        } else {
            viewHeightPx / density.density
        }

        LaunchedEffect(widthDp, heightDp, version) {
            if (widthDp <= 0f || heightDp <= 0f) return@LaunchedEffect

            val geometryData =
                createGeometryData(widthDp, heightDp, localBounds, globalBounds, density.density)

            // Restore environment and invoke the handler atomically in a single JS
            // eval so concurrent GeometryReader coroutines can't clobber each
            // other's `this.environment` between the restore and the call.
            handlerId?.let {
                childComponent = jsRuntime.callGeometryReaderComponent(
                    handlerId = handlerId,
                    data = geometryData,
                    environmentId = environmentId,
                )
            }
        }
        childComponent?.let { child ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = child,
                version = version,
                onUiEvent = onUiEvent
            )
        }
    }
}

private fun createGeometryData(
    width: Float,
    height: Float,
    localBounds: Rect,
    globalBounds: Rect,
    density: Float,
): Map<String, Any> {
    // Convert a pixel-space Rect to a dp-space frame dictionary matching
    // the iOS GeometryProxy.frame() format (flat dict with minX/minY/etc.).
    fun frameDictionary(rect: Rect): Map<String, Double> {
        val minX = (rect.left / density).toDouble()
        val minY = (rect.top / density).toDouble()
        val maxX = (rect.right / density).toDouble()
        val maxY = (rect.bottom / density).toDouble()
        val w = (rect.width / density).toDouble()
        val h = (rect.height / density).toDouble()
        return mapOf(
            "minX" to minX,
            "minY" to minY,
            "maxX" to maxX,
            "maxY" to maxY,
            "width" to w,
            "height" to h,
            "midX" to (minX + maxX) / 2.0,
            "midY" to (minY + maxY) / 2.0
        )
    }

    return mapOf(
        "size" to mapOf(
            "width" to width,
            "height" to height
        ),
        "safeAreaInsets" to mapOf(
            "top" to 0.0,
            "bottom" to 0.0,
            "leading" to 0.0,
            "trailing" to 0.0
        ),
        "local" to frameDictionary(localBounds),
        "global" to frameDictionary(globalBounds)
    )
}
