package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class ModifierProps(
    val modifier: ComponentModifier<*>?,
    val content: List<BaseComponent<*>?>?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "ModifierProps(modifier=$modifier, content=$content)"
    }
}

class ModifiedComponent(
    props: ModifierProps,
) : BaseComponent<ModifierProps>(props)
