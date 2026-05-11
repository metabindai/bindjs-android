package ai.metabind.bindjs.model

class ToggleComponent(
    props: ToggleComponentProps,
) : BaseComponent<ToggleComponentProps>(props)

class ToggleComponentProps(
    val isOn: Boolean = false,
    val setIsOnId: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children)