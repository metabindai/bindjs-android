package ai.metabind.bindjs.model

class NavigationLinkComponent(
    props: NavigationLinkComponentProps,
) : BaseComponent<NavigationLinkComponentProps>(props)

class NavigationLinkComponentProps(
    val destinationHandlerId: String,
    val label: BaseComponent<*>,
    children: List<BaseComponent<*>>?,
) : Props(children = children)