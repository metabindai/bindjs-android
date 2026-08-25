package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

/**
 * `.sheet({ isPresented, setIsPresented, content, onDismiss })` — mirrors
 * `SheetComponent` in bindjs-apple.
 *
 * Layout-neutral: the modified content renders exactly as it would without the
 * modifier, and the sheet is presented in its own window on top of it.
 *
 * The sheet's own subtree is NOT serialized with the tree — JS stores it as a
 * function and sends `contentHandlerId`. The renderer materializes it (see
 * `SheetModifier` in `BindJSView.kt`) only while the sheet is up, which is what
 * keeps a closed sheet's body off the render path entirely.
 */
class SheetModifier(
    props: SheetModifierProps,
) : ComponentModifier<SheetModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class SheetModifierProps(
    val isPresented: Boolean,
    val contentHandlerId: String?,
    val setIsPresentedHandlerId: String?,
    val dismissHandlerId: String?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "SheetModifierProps(isPresented=$isPresented, contentHandlerId=$contentHandlerId)"
    }
}
