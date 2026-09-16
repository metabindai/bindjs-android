package ai.metabind.bindjs.preview

import ai.metabind.bindjs.DesignerComponent
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.McpHost
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.EmptyComponent

/**
 * A [JsRuntime] with no JavaScript behind it, for rendering an already-built tree.
 *
 * `BindJSView` reaches back into the runtime in three places only: a GeometryReader
 * body, a sheet or menu body, and a custom button style. None of the committed fixture
 * trees use them, so every callback answers with nothing. A fixture that starts to
 * depend on one will render its slot empty, which the screenshot then shows.
 */
class FakeJsRuntime : JsRuntime {
    private val empty = EmptyComponent()

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
    override suspend fun restoreEnvironment(id: String) {}
    override suspend fun restoreEnvironmentOnly(id: String) {}
    override suspend fun restorePickerValue(currentValueId: String): String = ""
    override suspend fun callPickerSetter(setterId: String, value: String): String = ""
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
    ): BaseComponent<*>? = null

    override suspend fun awaitReady() {}
    override fun setOnRerenderRequested(listener: (() -> Unit)?) {}
    override fun close() {}
}
