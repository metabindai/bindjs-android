package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

// TODO, finish implementing this class once you have a working JS example
class IgnoresSafeAreaModifier(
    props: IgnoresSafeAreaProps,
) : ComponentModifier<IgnoresSafeAreaProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        // ignoresSafeArea in SwiftUI means content extends INTO the safe area.
        // On Android the default is no safe-area padding, so this is a no-op.
        return Modifier
    }
}

class IgnoresSafeAreaProps(
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
