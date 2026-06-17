package ai.metabind.bindjs

import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component

interface McpHost {
    fun openLink(url: String) {}
    fun sendMessage(message: String) {}
    fun updateModelContext(content: Map<String, Any?>) {}
    fun log(level: String, message: String) {}

    /**
     * Invoked when JS calls `host.toolCall(name, args)`. Return value is
     * serialized back to the awaiting JS promise as JSON. Throw to reject.
     * Default implementation rejects with "tool not implemented" so missing
     * tools surface clearly in JS rather than hanging the promise.
     */
    suspend fun toolCall(name: String, args: Map<String, Any?>): Any? {
        throw NotImplementedError("tool '$name' not implemented by host")
    }
}

interface JsRuntime {
    suspend fun setComponents(component: DesignerComponent)
    suspend fun willRender()
    suspend fun setMcpHost(host: McpHost?)
    suspend fun callComponent(name: String): BaseComponent<*>
    suspend fun callComponent(name: String, arguments: Map<String, Any?>?): BaseComponent<*>
    suspend fun callComponentPreview(name: String, previewIndex: Int = 0): BaseComponent<*>

    /**
     * Render a component atomically: `willRender()` + `callComponent()` under a
     * single lock hold so no event handler or other render can interleave
     * between them. The renderer walks one shared, mutable hook state that
     * `willRender()` resets and the component call consumes; splitting the pair
     * lets a concurrent call corrupt that state, leaving the rendered tree bound
     * to stale handler ids (taps/drags silently stop firing). Prefer this over
     * calling [willRender] and [callComponent] separately.
     */
    suspend fun renderComponent(name: String, arguments: Map<String, Any?>? = null): BaseComponent<*>

    /** Atomic [willRender] + [callComponentPreview]; see [renderComponent]. */
    suspend fun renderComponentPreview(name: String, previewIndex: Int = 0): BaseComponent<*>
    suspend fun callComponentThumbnail(name: String, isContent: Boolean = true): BaseComponent<*>
    suspend fun setEnvironment(environment: Map<String, Any>)
    suspend fun callEventHandler(handlerId: String, data: Array<Any> = emptyArray()): String?
    suspend fun callForResultComponent(handlerId: String): Component?
    suspend fun restoreForEachData(dataId: String): String
    suspend fun restoreEnvironment(id: String)
    suspend fun restoreEnvironmentOnly(id: String)
    suspend fun restorePickerValue(currentValueId: String): String
    suspend fun callPickerSetter(setterId: String, value: String): String
    suspend fun callForEachFunction(functionId: String, element: String, index: String): String?
    suspend fun callGeometryReaderComponent(
        handlerId: String,
        data: Map<String, Any>,
        environmentId: String?,
    ): BaseComponent<*>?

    suspend fun callButtonStyleHandler(
        handlerId: String,
        labelComponent: BaseComponent<*>,
        isPressed: Boolean,
    ): BaseComponent<*>?

    suspend fun awaitReady()

    /**
     * Register a listener that fires when JS state changes (`useState` /
     * `useStore` setters) and the rendered tree needs to be re-fetched.
     * Pass `null` to clear. The renderer typically responds by re-calling
     * [callComponent] with the same args and swapping the result into its
     * Compose state.
     */
    fun setOnRerenderRequested(listener: (() -> Unit)?)
}

