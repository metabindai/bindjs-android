package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class LineLimitModifier(
    props: LineLimitProps,
) : ComponentModifier<LineLimitProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class LineLimitProps(
    val rawValue: Int?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
