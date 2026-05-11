package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class TrackingModifier(
    props: TrackingProps,
) : ComponentModifier<TrackingProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class TrackingProps(
    val rawValue: Float?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
