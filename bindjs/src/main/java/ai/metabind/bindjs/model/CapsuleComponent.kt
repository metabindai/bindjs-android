package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps
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
 */
class StrokeStyle(
    val style: BaseComponent<*>? = null,
    val width: Float? = null,
) : Serializable {
    override fun toString(): String = "StrokeStyle(style=$style, width=$width)"
}
