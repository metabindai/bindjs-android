package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

/**
 * `.scrollContentBackground('hidden' | 'visible')` on a `List`: whether the list paints
 * its own backdrop (the grey behind the grouped cards). Hidden lets a `.background(...)`
 * written on the list show through. Read by `ListView`; contributes nothing to the
 * Compose `Modifier` of the node it is written on.
 */
class ScrollContentBackgroundModifier(
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
