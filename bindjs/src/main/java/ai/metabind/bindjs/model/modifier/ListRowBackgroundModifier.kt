package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent

/**
 * `.listRowBackground(view)` on a row of a `List`: the view drawn behind the whole row,
 * in place of the list's own row background. Read off the row's modifier chain by
 * `ListView`, which paints a `Color` or gradient directly and renders anything else
 * behind the row; contributes nothing to the row's own Compose `Modifier`, since the
 * row's padding and separators belong to the list, not to the row's content.
 */
class ListRowBackgroundModifier(
    props: BackgroundProps,
) : ComponentModifier<BackgroundProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }
}
