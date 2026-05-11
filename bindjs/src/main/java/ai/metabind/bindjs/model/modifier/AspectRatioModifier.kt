package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AspectRatioModifier(
    props: AspectRatioProps,
) : ComponentModifier<AspectRatioProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.aspectRatio(ratio = props.aspectRatio ?: 1f)
    }
}

class AspectRatioProps(
    val aspectRatio: Float?,
    val contentMode: String?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
