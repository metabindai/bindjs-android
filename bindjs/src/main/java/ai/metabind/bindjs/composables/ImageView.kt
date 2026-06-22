package ai.metabind.bindjs.composables

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getContentDescription
import ai.metabind.bindjs.composables.ext.getContentScale
import ai.metabind.bindjs.composables.ext.getNearestFontPointSize
import ai.metabind.bindjs.composables.ext.systemImage
import ai.metabind.bindjs.model.ImageComponent
import ai.metabind.bindjs.model.ext.toContentScale
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val TAG = "ImageView"

/** Network/image load timeout. Generated images can take a while to be ready server-side. */
private const val IMAGE_TIMEOUT_SECONDS = 70L

/** Fallback glyph size (pt) for a font-relative SVG icon with no resolvable
 *  `.font(...)` in its modifier chain — SwiftUI's default body size. */
private const val DEFAULT_ICON_POINT_SIZE = 17f

/** Whether an SVG declares its size in font-relative `em` units (e.g.
 *  `width="1em"`), the hallmark of an SF-Symbol glyph that should scale with the
 *  surrounding font rather than fill its container. */
private fun String.containsEmSizing(): Boolean =
    Regex("""(width|height)\s*=\s*"[\d.]+em"""").containsMatchIn(this)

@Volatile
private var sharedImageLoader: ImageLoader? = null
private val imageLoaderLock = Any()

/**
 * Decode a `data:` URI into raw bytes Coil can render. Coil 2.x has no built-in
 * support for `data:` URIs, so a base64-encoded image (e.g. the output of the
 * image-generator tool) fails to load and the image area renders empty. We decode
 * the payload here and hand Coil a [ByteArray], which it does support (PNG/JPEG via
 * the default decoder, SVG via the registered [SvgDecoder]).
 *
 * Returns null if [data] isn't a data URI or can't be decoded, so callers fall back
 * to passing the original string through.
 */
private fun decodeDataUri(data: String?): ByteArray? {
    if (data == null || !data.startsWith("data:")) return null
    val comma = data.indexOf(',')
    if (comma < 0) return null
    val header = data.substring(5, comma)
    val payload = data.substring(comma + 1)
    return try {
        if (header.contains("base64", ignoreCase = true)) {
            Base64.decode(payload, Base64.DEFAULT)
        } else {
            java.net.URLDecoder.decode(payload, "UTF-8").toByteArray()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode data URI", e)
        null
    }
}

/**
 * Process-wide Coil [ImageLoader] with extended timeouts. Coil's default OkHttp client
 * times out around 10s, which is too short for slow or server-generated images; a 70s
 * timeout gives the backend time to return the asset.
 */
private fun bindJsImageLoader(context: Context): ImageLoader {
    sharedImageLoader?.let { return it }
    return synchronized(imageLoaderLock) {
        sharedImageLoader ?: ImageLoader.Builder(context.applicationContext)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .callTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()
            }
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
            .also { sharedImageLoader = it }
    }
}

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
        val context = LocalContext.current
        // SF-Symbol glyphs arrive as SVGs sized in `em` (e.g. width="1em"),
        // meaning "scale to the current font size" — that's how SwiftUI sizes
        // them. Without a frame these images otherwise inherit fillMaxSize and
        // Coil stretches the glyph to fill its container (a star/heart ballooning
        // to the whole row/photo). Detect the em sizing and pin the icon to the
        // nearest font's point size, like iOS, instead of letting it fill.
        val isFontRelative = svg.containsEmSizing()
        val iconSizeDp = if (isFontRelative) {
            (modifiers.getNearestFontPointSize() ?: DEFAULT_ICON_POINT_SIZE).dp
        } else null
        val svgModifier = if (iconSizeDp != null) {
            modifiers.buildModifier(
                onUiEvent,
                exclude = listOf(
                    AccessibilityLabelModifier::class,
                    LocalModifier.FillMaxSize::class,
                    LocalModifier.FillMaxWidth::class,
                )
            ).then(Modifier.size(iconSizeDp))
        } else {
            modifiers.buildModifier(onUiEvent, exclude = listOf(AccessibilityLabelModifier::class))
        }
        AsyncImage(
            modifier = svgModifier,
            model = ImageRequest.Builder(context)
                .data(svg.toByteArray())
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            imageLoader = bindJsImageLoader(context),
            contentDescription = contentDescription,
            alignment = Alignment.Center,
            contentScale = if (iconSizeDp != null) ContentScale.Fit else contentScale,
            onError = { error ->
                Log.e(TAG, "Coil SVG Error $error")
            }
        )
    } else {
        val context = LocalContext.current
        val url = component.props.url
        // Coil 2.x can't load `data:` URIs directly — decode to bytes when present.
        val model: Any? = decodeDataUri(url) ?: url
        AsyncImage(
            modifier = modifiers.buildModifier(
                onUiEvent,
                exclude = listOf(AccessibilityLabelModifier::class)
            ),
            model = ImageRequest.Builder(context).data(model)
                .crossfade(true)
                .build(),
            imageLoader = bindJsImageLoader(context),
            contentDescription = contentDescription,
            alignment = Alignment.Center,
            contentScale = contentScale,
            onError = { error ->
                Log.e(TAG, "Coil image load error: ${error.result.throwable}")
            }
        )
    }
}
