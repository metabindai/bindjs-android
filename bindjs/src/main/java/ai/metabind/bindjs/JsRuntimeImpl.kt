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
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
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

    // Long-lived scope for resolving JS→native `host.toolCall(...)` requests.
    // SupervisorJob so one failing tool call doesn't kill the channel.
    private val toolCallScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        fun getInstance(context: Context): JsRuntime =
            instance ?: synchronized(this) {
                instance ?: JsRuntimeImpl(context.applicationContext).also { instance = it }
            }
    }

    init {
        if (!JavaScriptSandbox.isSupported()) {
            Log.e(TAG, "Javascript Engine is NOT supported!")
            throw Exception("Javascript Engine is NOT supported!")
        }
        gson = GsonProvider.get()
        initDeferred = CoroutineScope(Dispatchers.IO).async {
            val jsSandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).get()
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

            jsIsolate.evaluateJavaScriptAsync(mainScript).get()
        }
    }

    override suspend fun awaitReady() = initDeferred.await()

    override suspend fun callEventHandler(handlerId: String, data: Array<Any>): String? {
        try {
            val eventHandlerScript =
                "callEventHandler('$handlerId',${data.joinToString { gson.toJson(it) }});"
            return jsIsolate.evaluateJavaScriptAsync(eventHandlerScript).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading component", e)
            return null
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
            val result = jsIsolate.evaluateJavaScriptAsync(eventHandlerScript).await()
            val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
            val component: BaseComponent<*> = gson.fromJson(result, typeToken)
            printComponent(component)
            component
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
            val result = jsIsolate.evaluateJavaScriptAsync(eventHandlerScript).await()
            val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
            val component: BaseComponent<*> = gson.fromJson(result, typeToken)
            printComponent(component)
            component
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
            return jsIsolate.evaluateJavaScriptAsync(eventHandlerScript).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading component", e)
            return null
        }
    }

    override suspend fun restoreForEachData(dataId: String): String {
        val callScript = "restoreForEachData('$dataId');"
        return jsIsolate.evaluateJavaScriptAsync(callScript).await()
    }

    override suspend fun restoreEnvironment(id: String) {
        val script = "restoreEnvironment('$id',[]);"
        jsIsolate.evaluateJavaScriptAsync(script).await()
    }

    override suspend fun restoreEnvironmentOnly(id: String) {
        val script = "restoreEnvironmentOnly('$id');"
        jsIsolate.evaluateJavaScriptAsync(script).await()
    }

    override suspend fun restorePickerValue(currentValueId: String): String {
        val script = "restorePickerValue('$currentValueId',[]);"
        return jsIsolate.evaluateJavaScriptAsync(script).await()
    }

    override suspend fun callPickerSetter(setterId: String, value: String): String {
        val script = "callEventHandler('$setterId', '$value',[]);"
        val result = jsIsolate.evaluateJavaScriptAsync(script).await()
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
        jsIsolate.evaluateJavaScriptAsync(registerScript).await()
    }

    override suspend fun willRender() {
        val callScript = "willRender();"
        val result = jsIsolate.evaluateJavaScriptAsync(callScript).await()

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
        val result = jsIsolate.evaluateJavaScriptAsync(callScript).await()

        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)

        printComponent(component)

        return component
    }

    override suspend fun callComponentPreview(name: String, previewIndex: Int): BaseComponent<*> {
        Log.d(TAG, "Calling component preview: $name (index: $previewIndex)")
        val callScript = "callComponentPreview(['$name', $previewIndex]);"
        val result = jsIsolate.evaluateJavaScriptAsync(callScript).await()

        val typeToken = object : TypeToken<BaseComponent<*>>() {}.type
        val component: BaseComponent<*> = gson.fromJson(result, typeToken)

        printComponent(component)

        return component
    }

    override suspend fun callComponentThumbnail(name: String, isContent: Boolean): BaseComponent<*> {
        val thumbnailType = if (isContent) "content" else "component"
        Log.d(TAG, "Calling component thumbnail: $name (type: $thumbnailType)")
        val callScript =
            "callComponentThumbnail('$name', { type: '$thumbnailType', defaultPlatform: 'mobile', padding: 0 }, {}, [], true, []);"
        val result = jsIsolate.evaluateJavaScriptAsync(callScript).await()

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
                    componentName = "$componentName (val: ${component.props.rawValue})"
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
        jsIsolate.evaluateJavaScriptAsync(script).await()
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
                jsIsolate.evaluateJavaScriptAsync(script).await()
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
                jsIsolate.evaluateJavaScriptAsync("__fireTimer('$id');").await()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to fire timer $id back into JS", e)
            }
        }
    }

    override suspend fun setEnvironment(environment: Map<String, Any>) {
        Log.d(TAG, "Calling set environment: $environment")
        val callScript = "setEnvironment(['$environment']);"
        val result = jsIsolate.evaluateJavaScriptAsync(callScript).await()

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