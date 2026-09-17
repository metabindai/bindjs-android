package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

/**
 * `.listRowSeparator('hidden' | 'visible')` on a row of a `List`. Read off the row's
 * modifier chain by `ListView`, which then leaves out the dividers on both sides of the
 * row; contributes nothing to the row's own Compose `Modifier`.
 */
class ListRowSeparatorModifier(
    props: StringModifierProps,
) : ComponentModifier<StringModifierProps>(props) {
    val isHidden: Boolean
        get() = props.rawValue == "hidden"

    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }
}
