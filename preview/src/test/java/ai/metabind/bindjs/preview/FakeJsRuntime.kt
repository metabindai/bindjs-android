package ai.metabind.bindjs.preview

import ai.metabind.bindjs.DesignerComponent
import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.McpHost
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.EmptyComponent

/**
 * A [JsRuntime] with no JavaScript behind it, for rendering an already-built tree.
 *
 * `BindJSView` reaches back into the runtime in three places only: a GeometryReader
 * body, a sheet or menu body, and a custom button style. Every callback answers with
 * nothing, so a fixture that depends on one renders its slot empty (a styled button
 * falls back to its plain label), which the screenshot then shows.
 *
 * Calls that carry a picked value back into JavaScript are recorded in [calls], in
 * order, so a test can check what an event was routed to.
 */
class FakeJsRuntime : JsRuntime {
    private val empty = EmptyComponent()
    val calls = mutableListOf<String>()

    override suspend fun setComponents(component: DesignerComponent) {}
    override suspend fun willRender() {}
    override suspend fun setMcpHost(host: McpHost?) {}
    override suspend fun callComponent(name: String): BaseComponent<*> = empty
    override suspend fun callComponent(name: String, arguments: Map<String, Any?>?): BaseComponent<*> = empty
    override suspend fun callComponentPreview(name: String, previewIndex: Int): BaseComponent<*> = empty
    override suspend fun renderComponent(name: String, arguments: Map<String, Any?>?): BaseComponent<*> = empty
    override suspend fun renderComponentPreview(name: String, previewIndex: Int): BaseComponent<*> = empty
    override suspend fun evaluate(script: String): String = "null"
    override suspend fun renderExternal(script: String): BaseComponent<*> = empty
    override suspend fun callComponentThumbnail(name: String, isContent: Boolean): BaseComponent<*> = empty
    override suspend fun setEnvironment(environment: Map<String, Any>) {}
    override suspend fun callEventHandler(handlerId: String, data: Array<Any?>): String? = null
    override fun dispatchDragEvent(handlerId: String, state: Map<String, Any>) {}
    override suspend fun callForResultComponent(handlerId: String): Component? = null
    override suspend fun restoreForEachData(dataId: String): String = "[]"
    override suspend fun restoreEnvironment(id: String) { calls += "restoreEnvironment($id)" }
    override suspend fun restoreEnvironmentOnly(id: String) {}
    override suspend fun restorePickerValue(currentValueId: String): String = ""
    override suspend fun callPickerSetter(setterId: String, value: String): String {
        calls += "callPickerSetter($setterId, $value)"
        return ""
    }
    override suspend fun callForEachFunction(functionId: String, element: String, index: String): String? = null
    override suspend fun callGeometryReaderComponent(
        handlerId: String,
        data: Map<String, Any>,
        environmentId: String?,
    ): BaseComponent<*>? = null

    override suspend fun callButtonStyleHandler(
        handlerId: String,
        labelComponent: BaseComponent<*>,
        isPressed: Boolean,
    ): BaseComponent<*>? {
        // The real runtime serializes the label to hand it to the style. Do the same, so
        // a label that cannot be serialized fails the screenshot test instead of passing.
        GsonProvider.get().toJson(labelComponent, BaseComponent::class.java)
        return null
    }

    override suspend fun awaitReady() {}
    override fun setOnRerenderRequested(listener: (() -> Unit)?) {}
    override fun close() {}
}
