package ai.metabind.bindjs

import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component

interface McpHost {
    fun openLink(url: String) {}
    fun sendMessage(message: String) {}
    fun updateModelContext(content: Map<String, Any?>) {}
}

interface JsRuntime {
    suspend fun setComponents(component: DesignerComponent)
    suspend fun willRender()
    suspend fun setMcpHost(host: McpHost?)
    suspend fun callComponent(name: String): BaseComponent<*>
    suspend fun callComponent(name: String, arguments: Map<String, Any?>?): BaseComponent<*>
    suspend fun callComponentPreview(name: String, previewIndex: Int = 0): BaseComponent<*>
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
}

