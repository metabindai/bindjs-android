package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class CircleComponent(
    props: CircleComponentProps,
) : BaseComponent<CircleComponentProps>(props)

class CircleComponentProps(
    val fill: ForegroundStyleComponent?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "CircleComponentProps(fill=$fill)"
    }
}
