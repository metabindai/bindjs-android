package ai.metabind.bindjs.model

/**
 * `Placeholder({ name })` — a slot for a view the embedding app provides.
 *
 * The slot is resolved against the [ai.metabind.bindjs.composables.ComponentRegistry]
 * in the composition: when `name` is registered, the native composable renders with
 * the props and children of the enclosing component call; otherwise a neutral grey
 * rounded rectangle is drawn in its place. See PlaceholderView.
 */
class PlaceholderComponent(
    props: PlaceholderProps,
) : BaseComponent<PlaceholderProps>(props)

class PlaceholderProps(
    name: String?,
    children: List<BaseComponent<*>?>?,
) : Props(name = name, children = children)
