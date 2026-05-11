package ai.metabind.bindjs.composables

import android.util.Log
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getContentDescription
import ai.metabind.bindjs.composables.ext.getContentScale
import ai.metabind.bindjs.composables.ext.systemImage
import ai.metabind.bindjs.model.ImageComponent
import ai.metabind.bindjs.model.ext.toContentScale
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier

private const val TAG = "ImageView"

@Composable
fun ImageView(
    jsRuntime: JsRuntime,
    component: ImageComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val systemIconId = component.props.systemName.systemImage()
    val svg = component.props.svg?.trimStart()
    val contentMode = component.props.contentMode.toContentScale()
    val contentScale = contentMode ?: modifiers.getContentScale()
    val contentDescription = modifiers.getContentDescription()
    if (systemIconId != null) {
        Icon(
            painter = painterResource(id = systemIconId),
            contentDescription = ""
        )
    } else if (svg != null) {
        AsyncImage(
            modifier = modifiers.buildModifier(
                onUiEvent,
                exclude = listOf(AccessibilityLabelModifier::class)
            ),
            model = ImageRequest.Builder(LocalContext.current)
                .data(svg.toByteArray())
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            alignment = Alignment.Center,
            contentScale = contentScale,
            onError = { error ->
                Log.e(TAG, "Coil SVG Error $error")
            }
        )
    } else {
        AsyncImage(
            modifier = modifiers.buildModifier(
                onUiEvent,
                exclude = listOf(AccessibilityLabelModifier::class)
            ),
            model = ImageRequest.Builder(LocalContext.current).data(component.props.url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            alignment = Alignment.Center,
            contentScale = contentScale
        )
    }
}
