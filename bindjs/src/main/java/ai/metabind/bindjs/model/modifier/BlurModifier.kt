package ai.metabind.bindjs.model.modifier

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class BlurModifier(
    props: BlurProps,
) : ComponentModifier<BlurProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // SwiftUI's .blur lets the blurred content bleed past its layout
            // bounds. Compose's default (BlurredEdgeTreatment.Rectangle) clips
            // the result to the layout rectangle, turning soft glows (e.g.
            // blurred Circles used as gradient washes) into hard-edged blocks.
            // Unbounded matches iOS by not clipping the blur to the bounds.
            Modifier.blur(props.rawValue?.dp ?: 0f.dp, BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
    }
}

class BlurProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
