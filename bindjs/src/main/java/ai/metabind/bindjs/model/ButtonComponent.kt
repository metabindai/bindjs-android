package ai.metabind.bindjs.model

class ButtonComponent(
    props: ButtonProps,
) : BaseComponent<ButtonProps>(props)

class ButtonProps(
    val handlerId: String,
    val label: BaseComponent<*>,
) : Props(children = emptyList()) {
    override fun toString(): String {
        return "ButtonProps(handlerId='$handlerId')"
    }
}