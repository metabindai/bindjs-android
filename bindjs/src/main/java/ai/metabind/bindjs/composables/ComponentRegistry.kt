package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * A composable the embedding app supplies for a component name.
 *
 * Registered through [WithComponent] (or a [ComponentRegistry] provided via
 * [LocalComponentRegistry]), it renders in two places:
 *
 * - wherever a component of that name is called, in place of the component's body;
 * - wherever a component's body contains `Placeholder({ name })` with that name, in
 *   place of the placeholder.
 *
 * Either way the composable receives the props and children of the component call.
 */
fun interface ComponentRepresentable {
    @Composable
    fun Content(context: ComponentRepresentableContext)
}

/** What a [ComponentRepresentable] is handed: the call's props and children. */
class ComponentRepresentableContext internal constructor(
    /** The props the component was called with. Numbers arrive as [Double]. */
    val props: Map<String, Any?>,
    /** The children the component was called with, as component trees. */
    val children: List<BaseComponent<*>?>,
    private val jsRuntime: JsRuntime,
    private val version: Int,
    private val onUiEvent: (UiEvent) -> Unit,
) {
    /**
     * Renders [children] through the regular renderer. From a `Placeholder`, this is the
     * body of the component the placeholder sits in, with the placeholder itself
     * rendering its fallback.
     */
    @Composable
    fun Content() {
        children.forEach { child ->
            if (child != null) {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = child,
                    version = version,
                    onUiEvent = onUiEvent,
                )
            }
        }
    }
}

/** Native composables by component name. Immutable; [with] returns an extended copy. */
class ComponentRegistry(
    val components: Map<String, ComponentRepresentable> = emptyMap(),
) {
    fun with(name: String, component: ComponentRepresentable): ComponentRegistry =
        ComponentRegistry(components + (name to component))

    operator fun get(name: String?): ComponentRepresentable? = name?.let { components[it] }
}

/** The registry the renderer resolves component calls and placeholders against. */
val LocalComponentRegistry = compositionLocalOf { ComponentRegistry() }

/**
 * The innermost component call being rendered, so a `Placeholder` inside a component's
 * body can pick up that component's props and children.
 */
internal val LocalComponentCall = compositionLocalOf<Component?> { null }

/**
 * Names of the placeholders whose native composables are currently rendering. A
 * placeholder's context carries the body of the component it sits in, and that body
 * contains the placeholder itself; without this a native composable that renders
 * [ComponentRepresentableContext.Content] would resolve the same placeholder again
 * without end. Inside its own composable the placeholder renders its fallback instead.
 */
internal val LocalResolvingPlaceholders = compositionLocalOf<Set<String>> { emptySet() }

/**
 * Registers [component] under [name] for every BindJS view inside [content]. Calls
 * nest, each adding to the registry of the ones around it.
 */
@Composable
fun WithComponent(
    name: String,
    component: ComponentRepresentable,
    content: @Composable () -> Unit,
) {
    val registry = LocalComponentRegistry.current.with(name, component)
    CompositionLocalProvider(LocalComponentRegistry provides registry, content = content)
}

/** Renders [component] for [call], with the modifiers attached to the slot. */
@Composable
internal fun NativeComponentView(
    jsRuntime: JsRuntime,
    component: ComponentRepresentable,
    call: Component,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val context = remember(call, version) {
        ComponentRepresentableContext(
            props = call.props.propsMap,
            children = call.props.children ?: emptyList(),
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
        )
    }
    Box(modifier = modifiers.buildModifier(onUiEvent)) {
        component.Content(context)
    }
}
