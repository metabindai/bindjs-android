package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class RectangleComponent(
    props: RectangleComponentProps,
) : BaseComponent<RectangleComponentProps>(props)

class RectangleComponentProps(
    val fill: ForegroundStyleComponent?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "RectangleComponentProps(fill=$fill)"
    }
}

