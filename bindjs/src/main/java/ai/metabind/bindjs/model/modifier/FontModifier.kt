package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent

class FontModifier(
    props: FontProps,
) : ComponentModifier<FontProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return Modifier
    }

    override fun toString(): String {
        return "FontModifier(props=$props)"
    }
}

class FontProps(
    val rawValue: Any?,
    val custom: CustomFontProps? = null,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "FontProps(rawValue=$rawValue, custom=$custom)"
    }
}

/**
 * `.font(CustomFont({ family, size, url }))` — SwiftUI's `Font.custom`.
 *
 * The runtime lifts a `CustomFont` directive out of the `.font(...)` argument and hands
 * it over twice: as this typed `custom` object and, unchanged, as `rawValue` (see
 * `FontModifier` in script.js). Reading the typed copy keeps `rawValue`'s untyped Map
 * out of the size and family lookups.
 *
 * [url] is a remote font to download and register before use; it is carried here but
 * not yet honoured on Android, where a family that is not installed or bundled falls
 * back to the system font.
 */
class CustomFontProps(
    val family: String? = null,
    val size: Float? = null,
    val url: String? = null,
) : java.io.Serializable {
    override fun toString(): String = "CustomFontProps(family=$family, size=$size, url=$url)"
}
