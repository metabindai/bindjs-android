package ai.metabind.bindjs.composables

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

private const val TAG = "ComponentView"

@Composable
fun ForEachView(
    jsRuntime: JsRuntime,
    component: ForEachComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val children = component.props.children
    if (children?.isNotEmpty() == true) {
        Column {
            children.forEach { innerComponent ->
                innerComponent?.let {
                    BindJSView(
                        jsRuntime = jsRuntime,
                        component = innerComponent,
                        version = version,
                        onUiEvent = onUiEvent,
                        modifiers = modifiers,
                    )
                }
            }
        }
    } else {
        // expandForEach should be set to true, when BindJSRuntime is initialized.
        // i.e. const runtime = new BindJSRuntime({expandForEach: true});
        Log.e(TAG, "Rendering non-expanded ForEach component is not supported on Android.")
    }
}