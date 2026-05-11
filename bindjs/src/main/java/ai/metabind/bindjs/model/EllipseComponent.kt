package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class EllipseComponent(
    props: EllipseComponentProps,
) : BaseComponent<EllipseComponentProps>(props)

class EllipseComponentProps(
    val fill: ForegroundStyleComponent?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "EllipseComponentProps(fill=$fill)"
    }
}

