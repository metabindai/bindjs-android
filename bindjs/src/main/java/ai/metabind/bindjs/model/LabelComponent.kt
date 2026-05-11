package ai.metabind.bindjs.model

class LabelComponent(
    props: LabelProps,
) : BaseComponent<LabelProps>(props)

class LabelProps(
    val systemImage: String?,
    val title: BaseComponent<*>,
) : Props(children = emptyList())