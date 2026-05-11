package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class RoundedRectangleComponent(
    props: RoundedRectangleComponentProps,
) : BaseComponent<RoundedRectangleComponentProps>(props)

class RoundedRectangleComponentProps(
    val cornerRadius: Float? = 10.0f,
    val fill: ForegroundStyleComponent?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "RoundedRectangleComponentProps(cornerRadius=$cornerRadius, fill=$fill)"
    }
}
