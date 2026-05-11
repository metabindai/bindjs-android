package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent

class CornerRadiusModifier(
    props: IntModifierProps,
) : ComponentModifier<IntModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.clip(shape = RoundedCornerShape(props.rawValue.dp))
    }
}
