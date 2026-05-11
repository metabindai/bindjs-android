package ai.metabind.bindjs.composables

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.isEnabled
import ai.metabind.bindjs.model.VideoComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier

@OptIn(UnstableApi::class)
@Composable
fun VideoView(
    jsRuntime: JsRuntime,
    component: VideoComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val isEnabled = modifiers.isEnabled()

    val url = component.props.url!!
    val autoplay = component.props.autoplay
    val muted = component.props.muted
    val controls = component.props.controls
    val loop = component.props.loop
    val contentMode = component.props.contentMode

    val context = LocalContext.current

    val resizeMode = when (contentMode) {
        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = if (muted == true) 0f else 1f
            repeatMode = if (loop == true) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    val mediaSource = remember(url) { MediaItem.fromUri(url) }

    LaunchedEffect(mediaSource) {
        exoPlayer.setMediaItem(mediaSource)
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                setEnabled(isEnabled)
                player = exoPlayer
                useController = controls == true
                player?.playWhenReady = autoplay == true
                setResizeMode(resizeMode)
            }
        },
        modifier = modifiers.buildModifier(onUiEvent)
    )
}
