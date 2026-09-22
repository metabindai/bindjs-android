package ai.metabind.bindjs.model

class MenuComponent(
    props: MenuProps,
) : BaseComponent<MenuProps>(props)

class MenuProps(
    val label: BaseComponent<*>?,
    /** `Menu('Actions', [...])` sends its title as a string here rather than as `label`. */
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    /**
     * What the menu shows as its button: the `label` component, else a Text of the string
     * title, else "Menu" — bindjs-apple's fallback when a menu has no label at all.
     */
    val displayLabel: BaseComponent<*>
        get() = label ?: TextComponent(
            TextComponentProps(markdown = null, rawValue = rawValue ?: "Menu", children = null)
        )
}
