package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

/**
 * `.listStyle(...)` on a `List`: `automatic` / `insetGrouped` (the default look),
 * `grouped`, `inset`, `plain` or `sidebar`. Read by `ListView` from the chain wrapping the
 * list; contributes nothing to the Compose `Modifier` of the node it is written on.
 */
class ListStyleModifier(
    props: StringModifierProps,
) : ComponentModifier<StringModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }
}
