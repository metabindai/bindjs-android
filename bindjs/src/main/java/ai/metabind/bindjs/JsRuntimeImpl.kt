package ai.metabind.bindjs

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.ColorSchemeModifier
import ai.metabind.bindjs.model.modifier.FontModifier
import ai.metabind.bindjs.model.modifier.FontWeightModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class JsRuntimeImpl private constructor(
    context: Context,
) : JsRuntime {
    private lateinit var jsIsolate: JavaScriptIsolate
    private val gson: Gson
    private val initDeferred: Deferred<Unit>

    @Volatile
    private var mcpHost: McpHost? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // The JS isolate is single-threaded: `evaluateJavaScriptAsync` accepts
    // concurrent submissions but interleaving them corrupts the renderer's
    // mutable JS state (MET-1229 — rapid drag events racing with re-renders).
    // Serialize every JS entry point so one evaluation fully completes before
    // the next starts. A Mutex (not a single-thread dispatcher) is required
    // because `.await()` suspends and would otherwise release the thread mid-
    // evaluation, allowing a second eval to start. Use `evalJs` for all
    // isolate access except the one-time init script.
    private val jsLock = Mutex()

    // Long-lived scope for resolving JS→native `host.toolCall(...)` requests.
    // SupervisorJob so one failing tool call doesn't kill the channel.
    private val toolCallScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Drag-gesture backpressure (see [dispatchDragEvent]). A physical-device
    // pointer fires `changed` events (~168/sec) far faster than the jsLock +
    // render loop can drain (~30/sec); queuing each one builds an unbounded
    // backlog (MET-1229: 30+ second drag latency). Coalesce so only the newest
    // `changed` survives while the pipeline is busy, while `began`/`ended`/
    // `cancelled` stay ordered barriers. The queue and [dragDraining] are
    // guarded by the queue's own monitor; a single drain coroutine runs at a
    // time on [gestureScope].
    private val gestureScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dragQueue = ArrayDeque<DragEvent>()
    private var dragDraining = false

    private class DragEvent(
        val handlerId: String,
        val data: Array<Any>,
        val coalescable: Boolean,
    )

    @Volatile
    private var onRerenderRequested: (() -> Unit)? = null

    @Volatile
    private var rerenderPosted = false

    // setTimeout/setInterval scheduled from JS (the isolate has no event loop),
    // keyed by the JS-side timer id so clearTimeout/clearInterval can cancel
    // the pending main-handler post. See the timer bridge in script.js.
    private val timers = ConcurrentHashMap<String, Runnable>()

    override fun setOnRerenderRequested(listener: (() -> Unit)?) {
        onRerenderRequested = listener
    }

    companion object {
        private const val TAG = "JsRuntimeImpl"
        private const val MCP_PREFIX = "__MCP__::"

        @Volatile
        private var instance: JsRuntime? = null

        // Only one JavaScriptSandbox (a separate process) may be connected per
        // app process, so it's shared across all runtimes. Each runtime gets its
        // own *isolate* from it — isolates are independent JS contexts, cheap
        // relative to the sandbox, and give each runtime its own global state
        // (handler table, hook store, rerender listener, mcpHost).
        @Volatile
        private var sandboxDeferred: Deferred<JavaScriptSandbox>? = null

        private fun sandbox(context: Context): Deferred<JavaScriptSandbox> =
            sandboxDeferred ?: synchronized(this) {
                sandboxDeferred ?: CoroutineScope(Dispatchers.IO).async {
                    JavaScriptSandbox.createConnectedInstanceAsync(context.applicationContext).await()
                }.also { sandboxDeferred = it }
            }

        /** Process-wide shared runtime, for screens that render a single
         *  component tree at a time. */
        fun getInstance(context: Context): JsRuntime =
            instance ?: synchronized(this) {
                instance ?: JsRuntimeImpl(context.applicationContext).also { instance = it }
            }

        /**
         * Create an independent runtime with its own JS isolate. Use one per
         * concurrently-live component (e.g. each chat tool bubble) so their
         * handler ids, hook state, and rerender/host callbacks don't collide in
         * a shared isolate. Call [JsRuntime.close] when done to free the isolate.
         */
        fun create(context: Context): JsRuntime = JsRuntimeImpl(context.applicationContext)
    }

    init {
        if (!JavaScriptSandbox.isSupported()) {
            Log.e(TAG, "Javascript Engine is NOT supported!")
            throw Exception("Javascript Engine is NOT supported!")
        }
        gson = GsonProvider.get()
        initDeferred = CoroutineScope(Dispatchers.IO).async {
            val jsSandbox = sandbox(context).await()
            jsIsolate = jsSandbox.createIsolate()

            if (jsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING)) {
                jsIsolate.setConsoleCallback { message ->
                    val text = message.message
                    if (text != null && text.startsWith(MCP_PREFIX)) {
                        dispatchMcpMessage(text)
                    } else {
                        Log.d(TAG, "JSConsole: $text")
                    }
                }
            }

            val mainScript = loadMainScript(context)

            jsIsolate.evaluateJavaScriptAsync(mainScript).await()
        }
    }

    override fun close() {
        initDeferred.cancel()
        onRerenderRequested = null
        mcpHost = null
        timers.values.forEach { mainHandler.removeCallbacks(it) }
        timers.clear()
        toolCallScope.cancel()
        gestureScope.cancel()
        synchronized(dragQueue) {
            dragQueue.clear()
            dragDraining = false
        }
        try {
            if (::jsIsolate.isInitialized) jsIsolate.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing isolate", e)
        }
    }

    override suspend fun awaitReady() = initDeferred.await()

    /** Run a single JS evaluation, serialized against all other JS access. */
    private suspend fun evalJs(script: String): String =
        jsLock.withLock { evalLocked(script) }

    /** Evaluate assuming [jsLock] is already held — used to keep multi-call
     *  sequences (e.g. willRender + callComponent) atomic under one lock. */
    private suspend fun evalLocked(script: String): String =
        jsIsolate.evaluateJavaScriptAsync(script).await()

    /** willRender assuming [jsLock] is already held. */
    private suspend fun willRenderLocked() {
        val result = evalLocked("willRender();")
        Log.d(TAG, "willRender result:\n$result")
    }

    private fun parseComponent(result: String): BaseComponent<*> {
        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)
        printComponent(component)
        return component
    }

    override suspend fun callEventHandler(handlerId: String, data: Array<Any>): String? {
        try {
            val eventHandlerScript =
                "callEventHandler('$handlerId',${data.joinToString { gson.toJson(it) }});"
            return evalJs(eventHandlerScript)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading component", e)
            return null
        }
    }

    override fun dispatchDragEvent(handlerId: String, state: Map<String, Any>) {
        // Only the continuous middle of the gesture is safe to drop; phase
        // transitions carry state the handler must observe.
        val coalescable = state["phase"] == "changed"
        val event = DragEvent(handlerId, arrayOf(state), coalescable)
        val startDrain: Boolean
        synchronized(dragQueue) {
            val last = dragQueue.lastOrNull()
            if (coalescable && last != null && last.coalescable && last.handlerId == handlerId) {
                // Replace the queued `changed` — only the latest position matters.
                dragQueue[dragQueue.lastIndex] = event
            } else {
                dragQueue.addLast(event)
            }
            // Start a drain only if one isn't already running. Checking and
            // clearing [dragDraining] under the same monitor that the drain
            // uses to dequeue closes the lost-wakeup window.
            startDrain = !dragDraining
            if (startDrain) dragDraining = true
        }
        if (startDrain) gestureScope.launch { drainDragQueue() }
    }

    private suspend fun drainDragQueue() {
        while (true) {
            val next = synchronized(dragQueue) {
                val e = dragQueue.removeFirstOrNull()
                if (e == null) dragDraining = false
                e
            } ?: break
            // Serialized via jsLock inside callEventHandler; the handler's own
            // setState fires the (coalesced) rerender signal, so we don't
            // render here.
            callEventHandler(next.handlerId, next.data)
        }
    }

    override suspend fun callForResultComponent(handlerId: String): Component? {
        try {
            callEventHandler(handlerId = handlerId)?.let {
                val value = "{\"props\":{\"children\":[$it]}}"
                val typeToken = object : TypeToken<Component>() {}.type
                val component: Component = gson.fromJson(value, typeToken)
                printComponent(component)
                return component
            }
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading component", e)
            return null
        }
    }

    override suspend fun callButtonStyleHandler(
        handlerId: String,
        labelComponent: BaseComponent<*>,
        isPressed: Boolean,
    ): BaseComponent<*>? {
        val label = gson.toJson(labelComponent)
        return try {
            val eventHandlerScript = "callButtonStyleHandler('$handlerId', $label, $isPressed);"
            val result = evalJs(eventHandlerScript)
            val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
            val component: BaseComponent<*> = gson.fromJson(result, typeToken)
            printComponent(component)
            component
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse component from callButtonStyleHandler", e)
            null
        }
    }

    override suspend fun callGeometryReaderComponent(
        handlerId: String,
        data: Map<String, Any>,
        environmentId: String?,
    ): BaseComponent<*>? {
        return try {
            val jsonData = gson.toJson(data)
            val envArg = if (environmentId != null) "'$environmentId'" else "null"
            val eventHandlerScript =
                "callGeometryReaderComponent('$handlerId', $jsonData, $envArg);"
            val result = evalJs(eventHandlerScript)
            val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
            val component: BaseComponent<*> = gson.fromJson(result, typeToken)
            printComponent(component)
            component
        } catch (e: CancellationException) {
            // A composition-scoped caller (LaunchedEffect keyed on `version`) is
            // cancelled on every re-render. Swallowing that into `null` told the
            // caller "the handler produced nothing", so it wiped content that was
            // merely being recomputed — an animating GeometryReader lost its child
            // on every frame. Cancellation is the caller's business; rethrow it.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse component from callGeometryReaderComponent", e)
            null
        }
    }

    override suspend fun callForEachFunction(
        functionId: String,
        element: String,
        index: String,
    ): String? {
        try {
            val eventHandlerScript = "callForEachFunction('$functionId','$element','$index');"
            return evalJs(eventHandlerScript)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading component", e)
            return null
        }
    }

    override suspend fun restoreForEachData(dataId: String): String {
        val callScript = "restoreForEachData('$dataId');"
        return evalJs(callScript)
    }

    override suspend fun restoreEnvironment(id: String) {
        val script = "restoreEnvironment('$id',[]);"
        evalJs(script)
    }

    override suspend fun restoreEnvironmentOnly(id: String) {
        val script = "restoreEnvironmentOnly('$id');"
        evalJs(script)
    }

    override suspend fun restorePickerValue(currentValueId: String): String {
        val script = "restorePickerValue('$currentValueId',[]);"
        return evalJs(script)
    }

    override suspend fun callPickerSetter(setterId: String, value: String): String {
        val script = "callEventHandler('$setterId', '$value',[]);"
        val result = evalJs(script)
        Log.d(TAG, "callPickerSetter result: $result")

        return result
    }

    override suspend fun setComponents(component: DesignerComponent) {
        component.dependencies.forEach {
            setComponents(it)
        }

        val contentJson = gson.toJson(component.content)
        val componentObject = "{'${component.name}': $contentJson}"

        val registerScript = "setComponents($componentObject);"
        evalJs(registerScript)
    }

    override suspend fun willRender() {
        val callScript = "willRender();"
        val result = evalJs(callScript)

        Log.d(TAG, "willRender result:\n$result")
    }

    override suspend fun callComponent(name: String): BaseComponent<*> {
        return callComponent(name, null)
    }

    override suspend fun callComponent(
        name: String,
        arguments: Map<String, Any?>?,
    ): BaseComponent<*> {
        Log.d(TAG, "Calling component: $name (args=${arguments?.keys})")
        val argsJson = gson.toJson(arguments ?: emptyMap<String, Any?>())
        val callScript = "callComponent(['$name', $argsJson]);"
        val result = evalJs(callScript)

        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)

        printComponent(component)

        return component
    }

    override suspend fun callComponentPreview(name: String, previewIndex: Int): BaseComponent<*> {
        Log.d(TAG, "Calling component preview: $name (index: $previewIndex)")
        val callScript = "callComponentPreview(['$name', $previewIndex]);"
        val result = evalJs(callScript)

        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)

        printComponent(component)

        return component
    }

    override suspend fun renderComponent(
        name: String,
        arguments: Map<String, Any?>?,
    ): BaseComponent<*> = jsLock.withLock {
        Log.d(TAG, "Rendering component: $name (args=${arguments?.keys})")
        willRenderLocked()
        val argsJson = gson.toJson(arguments ?: emptyMap<String, Any?>())
        parseComponent(evalLocked("callComponent(['$name', $argsJson]);"))
    }

    override suspend fun renderComponentPreview(
        name: String,
        previewIndex: Int,
    ): BaseComponent<*> = jsLock.withLock {
        Log.d(TAG, "Rendering component preview: $name (index: $previewIndex)")
        willRenderLocked()
        parseComponent(evalLocked("callComponentPreview(['$name', $previewIndex]);"))
    }

    override suspend fun callComponentThumbnail(name: String, isContent: Boolean): BaseComponent<*> {
        val thumbnailType = if (isContent) "content" else "component"
        Log.d(TAG, "Calling component thumbnail: $name (type: $thumbnailType)")
        val callScript =
            "callComponentThumbnail('$name', { type: '$thumbnailType', defaultPlatform: 'mobile', padding: 0 }, {}, [], true, []);"
        val result = evalJs(callScript)

        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)

        printComponent(component)

        return component
    }

    private fun printComponent(component: BaseComponent<*>?) {
        val sb = StringBuilder()
        sb.appendLine("=== Component Tree ===")
        printComponentRecursive(component, sb, indent = 0, modifiers = emptyList())
        sb.appendLine("======================")
        Log.d(TAG, sb.toString())
    }

    private fun printComponentRecursive(
        component: BaseComponent<*>?,
        sb: StringBuilder,
        indent: Int,
        modifiers: List<String>,
    ) {
        if (component == null) return

        val indentStr = "  ".repeat(indent)

        when (component) {
            is ModifiedComponent -> {
                val modifierName = component.props.modifier?.let {
                    when (it) {
                        is PaddingModifier,
                        is ForegroundStyleModifier,
                        is FrameModifier,
                        is FontWeightModifier,
                        is FontModifier,
                        is BackgroundModifier,
                        is ColorSchemeModifier,
                            -> it.toString()

                        else ->
                            it::class.simpleName ?: "UnknownModifier"
                    }
                } ?: "NullModifier"

                val newModifiers = modifiers + modifierName

                component.props.content?.forEach { child ->
                    printComponentRecursive(child, sb, indent, newModifiers)
                }
            }

            else -> {
                var componentName = component::class.simpleName ?: "UnknownComponent"
                val modifiersStr = if (modifiers.isNotEmpty()) {
                    " [${modifiers.joinToString(", ")}]"
                } else {
                    ""
                }
                if (component is TextComponent) {
                    componentName = "$componentName (val: ${component.props.rawValue} markdown: ${component.props.markdown})"
                }
                sb.appendLine("$indentStr$componentName$modifiersStr")

                component.props.children?.forEach { child ->
                    printComponentRecursive(child, sb, indent + 1, emptyList())
                }
            }
        }
    }

    override suspend fun setMcpHost(host: McpHost?) {
        awaitReady()
        mcpHost = host
        val script = if (host != null) "setMcpHost(true);" else "setMcpHost(false);"
        evalJs(script)
    }

    private fun dispatchMcpMessage(text: String) {
        val payload = text.removePrefix(MCP_PREFIX)
        val sepIdx = payload.indexOf("::")
        if (sepIdx < 0) return
        val method = payload.substring(0, sepIdx)
        val argsJson = payload.substring(sepIdx + 2)

        // The rerender signal is host-independent — it must fire even when
        // no McpHost is registered (e.g. previews) so the renderer can still
        // refresh the tree after state changes. JS emits this synchronously
        // (one console message per setState call), so coalesce bursts here:
        // we only post once to the main handler at a time, and the posted
        // runnable picks up whatever state JS has at the moment it runs.
        if (method == "__rerender__") {
            if (rerenderPosted) return
            rerenderPosted = true
            mainHandler.post {
                rerenderPosted = false
                try {
                    onRerenderRequested?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "rerender listener threw", e)
                }
            }
            return
        }

        // Timers are host-independent (they must fire in previews too, matching
        // iOS's JSTimers): schedule/cancel a main-handler post and call back into
        // JS via __fireTimer(id) when it elapses.
        when (method) {
            "setTimeout", "setInterval" -> {
                val timerArgs = parseArgs(argsJson) ?: return
                val id = timerArgs.getOrNull(0) as? String ?: return
                val delayMs = (timerArgs.getOrNull(1) as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
                scheduleTimer(id, delayMs, repeats = method == "setInterval")
                return
            }
            "clearTimeout" -> {
                val timerArgs = parseArgs(argsJson) ?: return
                val id = timerArgs.getOrNull(0) as? String ?: return
                timers.remove(id)?.let { mainHandler.removeCallbacks(it) }
                return
            }
        }

        val host = mcpHost ?: return
        val args = try {
            gson.fromJson(argsJson, Array<Any?>::class.java) ?: return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse MCP args: $text", e)
            return
        }
        mainHandler.post {
            try {
                when (method) {
                    "openLink" -> (args.getOrNull(0) as? String)?.let { host.openLink(it) }
                    "sendMessage" -> (args.getOrNull(0) as? String)?.let { host.sendMessage(it) }
                    "updateModelContext" -> {
                        @Suppress("UNCHECKED_CAST")
                        (args.getOrNull(0) as? Map<String, Any?>)?.let {
                            host.updateModelContext(it)
                        }
                    }
                    "log" -> {
                        val level = args.getOrNull(0) as? String ?: "info"
                        val payload = args.drop(1).joinToString(" ") { arg ->
                            when (arg) {
                                null -> "null"
                                is String -> arg
                                else -> gson.toJson(arg)
                            }
                        }
                        host.log(level, payload)
                    }
                    "toolCall" -> dispatchToolCall(host, args)
                    else -> Log.w(TAG, "Unknown MCP method: $method")
                }
            } catch (e: Exception) {
                Log.e(TAG, "MCP handler threw for method: $method", e)
            }
        }
    }

    /**
     * Handle a JS-originated `host.toolCall(name, args)`. The JS shim packs
     * `[requestId, toolName, toolArgs]` into the MCP payload; we run the
     * host's suspend handler off the main thread and resolve the awaiting
     * JS promise via `__resolveToolCall(id, ok, value)`.
     */
    private fun dispatchToolCall(host: McpHost, args: Array<Any?>) {
        val id = args.getOrNull(0) as? String
        val name = args.getOrNull(1) as? String
        if (id == null || name == null) {
            Log.w(TAG, "toolCall missing id/name: ${args.toList()}")
            return
        }
        @Suppress("UNCHECKED_CAST")
        val toolArgs = (args.getOrNull(2) as? Map<String, Any?>) ?: emptyMap()
        toolCallScope.launch {
            val (ok, payload) = try {
                true to host.toolCall(name, toolArgs)
            } catch (e: Throwable) {
                Log.e(TAG, "host.toolCall('$name') threw", e)
                false to (e.message ?: e::class.simpleName ?: "tool call failed")
            }
            try {
                val idJson = gson.toJson(id)
                val payloadJson = gson.toJson(payload)
                val script = "__resolveToolCall($idJson, $ok, $payloadJson);"
                evalJs(script)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to resolve toolCall '$name' (id=$id) back into JS", e)
            }
        }
    }

    private fun parseArgs(argsJson: String): Array<Any?>? = try {
        gson.fromJson(argsJson, Array<Any?>::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse timer args: $argsJson", e)
        null
    }

    /**
     * Schedule a JS-originated timer. A one-shot removes itself after firing;
     * a repeating timer re-posts itself until cancelled via clearInterval/
     * clearTimeout. Re-using an id replaces the prior post.
     */
    private fun scheduleTimer(id: String, delayMs: Long, repeats: Boolean) {
        val runnable = object : Runnable {
            override fun run() {
                if (repeats) {
                    // Keep the same instance registered so clear* can still find it.
                    mainHandler.postDelayed(this, delayMs)
                } else {
                    timers.remove(id)
                }
                fireTimer(id)
            }
        }
        timers.put(id, runnable)?.let { mainHandler.removeCallbacks(it) }
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun fireTimer(id: String) {
        toolCallScope.launch {
            try {
                evalJs("__fireTimer('$id');")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to fire timer $id back into JS", e)
            }
        }
    }

    override suspend fun setEnvironment(environment: Map<String, Any>) {
        // Two bugs lived in the old one-liner (`setEnvironment(['$environment'])`):
        //   1. `'$environment'` interpolated Kotlin's Map.toString() — i.e.
        //      `{toolName=Carousel, toolResult={...}}` — into a single-quoted JS
        //      string, so the JS never saw a real object.
        //   2. The value was wrapped in a JS array. iOS's bridge passes the env dict
        //      as the single argument, and the JS side
        //      (`setEnvironment: (environment) => runtime.registerEnvironment(environment)`)
        //      expects exactly that — not an array.
        // Effect: env was never populated on Android, so components reading
        // `env.toolResult` / `env.toolArguments` (e.g. a product carousel pulling
        // product data out of a tool result) ran with no inputs and rendered empty.
        val envJson = gson.toJson(environment)
        Log.d(TAG, "Calling set environment: $envJson")
        val callScript = "setEnvironment($envJson);"
        val result = evalJs(callScript)

        Log.d(TAG, "setEnvironment result: $result")
    }

    private suspend fun loadMainScript(context: Context): String {
        return withContext(Dispatchers.IO) {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.script)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            String(buffer)
        }
    }
}