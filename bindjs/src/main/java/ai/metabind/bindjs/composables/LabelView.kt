package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.systemIcon
import ai.metabind.bindjs.composables.ext.systemImage
import ai.metabind.bindjs.model.LabelComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.props.verticalAlignment

@Composable
fun LabelView(
    jsRuntime: JsRuntime,
    component: LabelComponent,
    modifiers: List<ComponentModifier<*>>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val systemIconId = component.props.systemImage.systemImage()
    val systemIcon = if (systemIconId == null) component.props.systemImage.systemIcon() else null
    Row(
        modifier = modifiers
            .buildModifier(onUiEvent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (systemIconId != null || systemIcon != null) {
            if (systemIconId != null) {
                Icon(
                    painter = painterResource(id = systemIconId),
                    contentDescription = ""
                )
            } else {
                Icon(
                    imageVector = systemIcon!!,
                    contentDescription = ""
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        BindJSView(
            jsRuntime = jsRuntime,
            component = component.props.title,
            version = version,
            onUiEvent = onUiEvent
        )
    }
}
