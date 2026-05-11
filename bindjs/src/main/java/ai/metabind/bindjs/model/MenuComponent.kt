package ai.metabind.bindjs.model

class MenuComponent(
    props: MenuProps,
) : BaseComponent<MenuProps>(props)

class MenuProps(
    val label: BaseComponent<*>?,
    children: List<BaseComponent<*>>?,
) : Props(children = children)
