package ai.metabind.bindjs.model

/**
 * `ToolbarItem({ placement }, content)` and `ToolbarItemGroup`. Only meaningful
 * inside a `.toolbar(...)`; rendered by `NavigationStackView`, never on its own.
 */
class ToolbarItemComponent(
    props: ToolbarItemProps,
) : BaseComponent<ToolbarItemProps>(props)

class ToolbarItemGroupComponent(
    props: ToolbarItemProps,
) : BaseComponent<ToolbarItemProps>(props)

class ToolbarItemProps(
    val placement: String?,
    /** JS may pass the item's view as a `content` prop or as children; both occur. */
    val content: BaseComponent<*>?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    /** The views this item contributes to the bar, in order, with a `ForEach` read through. */
    val items: List<BaseComponent<*>>
        get() = (content?.let { listOf(it) } ?: children)
            .expandingForEach()?.filterNotNull()
            ?: emptyList()

    override fun toString(): String {
        return "ToolbarItemProps(placement=$placement)"
    }
}
