package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

sealed class LocalModifier(
    val modifier: Modifier
) : ComponentModifier<ComponentModifierProps>(ComponentModifierProps(emptyList())) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return modifier
    }

    override fun toString(): String {
        return "${this::class.simpleName}"
    }

    class Weight(modifier: Modifier) : LocalModifier(modifier)
    class MatchParentSize(modifier: Modifier) : LocalModifier(modifier)
    class FillMaxWidth(modifier: Modifier) : LocalModifier(modifier)
    class FillMaxSize(modifier: Modifier) : LocalModifier(modifier)
    class WrapContentSize(modifier: Modifier) : LocalModifier(modifier)
    class InRow(modifier: Modifier) : LocalModifier(modifier)
}
