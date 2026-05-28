package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AccessibilityHintModifier(
    props: AccessibilityHintModifierProps,
) : ComponentModifier<AccessibilityHintModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier = Modifier
}

class AccessibilityHintModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: String? = null,
) : ComponentModifierProps(children)
