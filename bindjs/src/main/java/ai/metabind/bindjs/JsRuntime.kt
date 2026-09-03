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

    /**
     * Evaluate [script] in this runtime's isolate and return its completion value.
     *
     * The seam a renderer that is not a BindJS component uses to reach the runtime —
     * an A2UI interpreter, say, which arrives as its own bundle, installs a global, and
     * has to register its catalog on *this* instance. It cannot bring its own: a
     * `handlerId` in the AST only resolves back to a closure inside the instance that
     * stored it, and hook state is keyed by component path within that instance, so a
     * second runtime draws a correct-looking tree that does nothing when tapped.
     *
     * Serialized against every other JS entry point, like the rest of this interface.
     * Note that `script.js` declares `runtime` as a `const`, so it lives in the global
     * lexical environment: evaluated source can name it, and it is the [BindJSRuntime]
     * itself rather than the facade of globals wrapped around it.
     *
     * Use [renderExternal] for anything that builds a tree — this does not reset hook
     * state, and must not be used to call a component.
     */
    suspend fun evaluate(script: String): String

    /**
     * Atomic `willRender()` + [evaluate], for a renderer that builds its own AST.
     *
     * The [renderComponent] counterpart for trees that do not come from a registered
     * component, and it exists for the same reason: `willRender()` resets the
     * component-path counters hook state is keyed by, so a tree built before it — or two
     * trees built between one reset and one decode — binds this pass's hooks to the last
     * pass's paths. Holding the lock across both is what makes that ordering
     * unstateable-wrong rather than a rule in a doc comment.
     *
     * [script] must evaluate to the AST as a JSON string, which is what this runtime's
     * own `callComponent` returns and what Gson decodes here.
     */
    suspend fun renderExternal(script: String): BaseComponent<*>
    suspend fun callComponentThumbnail(name: String, isContent: Boolean = true): BaseComponent<*>
    suspend fun setEnvironment(environment: Map<String, Any>)
    // Arguments are nullable: a chart selection that has been cleared calls its
    // handler with `null`, the way iOS hands the binding's nil straight through.
    suspend fun callEventHandler(handlerId: String, data: Array<Any?> = emptyArray()): String?

    /**
     * Dispatch a drag-gesture event with backpressure so a fast pointer can't
     * outrun the serialized JS pipeline (MET-1229).
     *
     * The continuous `changed` phase is coalesced latest-wins: while the JS
     * lock + render loop is busy, a newer `changed` overwrites the queued one
     * instead of piling up, so a physical device emitting ~168 moves/sec
     * collapses to whatever the pipeline can actually drain (~30/sec) with no
     * growing backlog or latency. The `began`, `ended` and `cancelled` phases
     * are barriers — never dropped, order preserved — so the handler always
     * sees a well-formed gesture.
     *
     * Fire-and-forget: the handler's own state change drives the re-render via
     * the listener registered with [setOnRerenderRequested], so callers must
     * NOT render explicitly per event. For discrete events that must never be
     * dropped (tap, change, appear) use [callEventHandler] instead.
     */
    fun dispatchDragEvent(handlerId: String, state: Map<String, Any>)
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

    /**
     * Release this runtime's JS isolate and listeners. Call when a per-instance
     * runtime (see `JsRuntimeImpl.create`) is no longer needed — e.g. a chat
     * bubble leaving composition — so its isolate and handler/hook state are
     * freed. Safe to call before the isolate finishes initializing. The shared
     * process-wide sandbox is not affected. No-op semantics after close.
     */
    fun close()
}

