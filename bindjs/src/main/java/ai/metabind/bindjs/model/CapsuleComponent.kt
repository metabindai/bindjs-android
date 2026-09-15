package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class CapsuleComponent(
    props: CapsuleComponentProps,
) : BaseComponent<CapsuleComponentProps>(props)


class CapsuleComponentProps(
    val fill: ForegroundStyleComponent?,
    val stroke: StrokeStyle? = null,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "CapsuleComponentProps(fill=$fill, stroke=$stroke)"
    }
}

/**
 * A shape's `.stroke(style, lineWidth:)` — an outline rather than a fill. The
 * `style` resolves to a [ColorComponent] or brush via the registered runtime
 * type adapter (it carries its own `type` discriminator).
 *
 * The runtime spells the width `lineWidth` (SwiftUI's own label, and the only
 * key the `Stroke` shim in script.js ever writes — a bare `.stroke(2)` becomes
 * `{ lineWidth: 2 }`), so the bare `width` this field used to bind to never
 * arrived and every stroke fell back to the 1dp default.
 */
class StrokeStyle(
    val style: BaseComponent<*>? = null,
    @SerializedName(value = "lineWidth", alternate = ["width"])
    val width: Float? = null,
) : Serializable {
    override fun toString(): String = "StrokeStyle(style=$style, width=$width)"
}
