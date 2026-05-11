package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class PickerComponentProps(
    val label: String,
    val selection: List<String>,
    val currentValueId: String?,
    val setterId: String?,
    val environmentId: String,
    children: List<BaseComponent<*>>?
) : ComponentModifierProps(children)

class PickerComponent(
    props: PickerComponentProps,
): BaseComponent<PickerComponentProps>(props)
