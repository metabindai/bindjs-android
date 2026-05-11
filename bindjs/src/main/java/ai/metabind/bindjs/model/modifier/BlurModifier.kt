package ai.metabind.bindjs.model.modifier

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            Modifier.blur(props.rawValue?.dp ?: 0f.dp)
        } else {
            Modifier
        }
    }
}

class BlurProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
