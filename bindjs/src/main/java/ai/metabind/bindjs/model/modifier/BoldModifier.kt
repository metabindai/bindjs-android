package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

class BoldModifier(
    props: StringModifierProps,
) : ComponentModifier<StringModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        // TODO
        return Modifier
    }

    override fun toString(): String {
        return "BoldModifier(props=$props)"
    }
}
