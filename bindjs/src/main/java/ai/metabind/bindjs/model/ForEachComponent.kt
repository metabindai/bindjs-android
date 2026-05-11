package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class ForEachComponent(
    props: ForEachComponentProps,
) : BaseComponent<ForEachComponentProps>(props)

class ForEachComponentProps(
    val dataId: String,
    val count: Int,
    val functionId: String,
    val environmentId: String,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    
    override fun toString(): String {
        return "ForEachComponentProps(dataId='$dataId', count=$count, functionId='$functionId', environmentId='$environmentId')"
    }
}

