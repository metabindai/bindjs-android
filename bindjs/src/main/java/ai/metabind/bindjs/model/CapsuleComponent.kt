package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class CapsuleComponent(
    props: CapsuleComponentProps,
) : BaseComponent<CapsuleComponentProps>(props)


class CapsuleComponentProps(
    val fill: ForegroundStyleComponent?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "CapsuleComponentProps(fill=$fill)"
    }
}

