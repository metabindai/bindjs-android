package ai.metabind.bindjs.model.modifier

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class AspectRatioModifier(
    props: AspectRatioProps,
) : ComponentModifier<AspectRatioProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier.aspectRatio(ratio = props.rawValue ?: 1f)
    }
}

class AspectRatioProps(
    // The JS bridge serializes the ratio as `rawValue` (the shared modifier-value
    // convention), so deserialize from that key — accepting `aspectRatio` as an
    // alternate. Reading the wrong key left this null and fell back to `1f`
    // (a square), which made `.aspectRatio(1.72)` photos render as 1:1 and crowd
    // out their sibling content.
    @SerializedName(value = "rawValue", alternate = ["aspectRatio"])
    val rawValue: Float?,
    val contentMode: String?,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)
