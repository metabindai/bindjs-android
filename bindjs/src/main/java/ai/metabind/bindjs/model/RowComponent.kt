package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.props.LayoutProps

open class RowComponent(
    props: LayoutProps,
) : BaseComponent<LayoutProps>(props)

/** SwiftUI's `LazyHStack`. See [LazyColumnComponent] for why this is a subclass. */
class LazyRowComponent(
    props: LayoutProps,
) : RowComponent(props)
