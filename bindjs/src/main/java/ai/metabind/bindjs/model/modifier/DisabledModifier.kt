package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class DisabledModifier(
    props: DisabledModifierProps,
) : ComponentModifier<DisabledModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class DisabledModifierProps(
    /**
     * The modifier's argument. JS always sends one — a bare `.disabled()` is filled in
     * with `true` by `modifierDefaults` — but a missing value is read as `true` so the
     * modifier can never be present and mean nothing.
     */
    val rawValue: Boolean? = null,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)
