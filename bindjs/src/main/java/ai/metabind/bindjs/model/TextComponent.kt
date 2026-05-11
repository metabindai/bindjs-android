package ai.metabind.bindjs.model

class TextComponent(
    props: TextComponentProps,
) : BaseComponent<TextComponentProps>(props)

class TextComponentProps(
    val markdown: String?,
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "TextComponentProps(rawValue=$rawValue, markdown=$markdown)"
    }
}