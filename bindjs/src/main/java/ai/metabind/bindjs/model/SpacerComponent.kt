package ai.metabind.bindjs.model

class SpacerComponent(
    props: SpacerComponentProps
): BaseComponent<SpacerComponentProps>(props)

class SpacerComponentProps(
    val minLength: Float?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "SpacerComponentProps(minLength=$minLength)"
    }
}