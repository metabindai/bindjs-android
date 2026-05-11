package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AllowsHitTestingModifier(
    props: AllowsHitTestingModifierProps,
) : ComponentModifier<AllowsHitTestingModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class AllowsHitTestingModifierProps(
    val rawValue: Boolean,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
