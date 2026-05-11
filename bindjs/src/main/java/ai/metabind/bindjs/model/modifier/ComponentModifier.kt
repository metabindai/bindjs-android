package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Props
import java.io.Serializable

abstract class ComponentModifier<T : ComponentModifierProps>(val props: T) : Serializable {
    @Composable
    abstract fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier

    override fun toString(): String {
        return "${this::class.simpleName} Props: $props"
    }
}

open class ComponentModifierProps(
    children: List<BaseComponent<*>?>?,
) : Props(children = children)
