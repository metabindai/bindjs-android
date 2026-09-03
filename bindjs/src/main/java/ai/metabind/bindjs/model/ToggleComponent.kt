package ai.metabind.bindjs.model

class ToggleComponent(
    props: ToggleComponentProps,
) : BaseComponent<ToggleComponentProps>(props)

class ToggleComponentProps(
    // SwiftUI's `Toggle(label, isOn:)` draws its label, and bindjs-apple's
    // ToggleComponent carries one. Without the field here Gson drops it on the floor,
    // so a labelled toggle arrives as a bare switch — see ToggleView.
    val label: String? = null,
    val isOn: Boolean = false,
    val setIsOnId: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children)
