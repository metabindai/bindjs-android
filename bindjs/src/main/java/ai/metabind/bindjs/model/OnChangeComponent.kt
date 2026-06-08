package ai.metabind.bindjs.model

/**
 * SwiftUI `.onChange(of:)` — a transparent watcher node. The JS serializes it
 * as a component (not a leaf modifier) carrying the watched `value` (a string)
 * and the `handlerId` to invoke when that value changes. It renders its
 * children (usually none) inline; the change detection lives in OnChangeView.
 */
class OnChangeComponent(
    props: OnChangeProps,
) : BaseComponent<OnChangeProps>(props)

class OnChangeProps(
    val value: String?,
    val handlerId: String,
    children: List<BaseComponent<*>?>?,
) : Props(children = children)
